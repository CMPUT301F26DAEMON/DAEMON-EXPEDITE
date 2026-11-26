"use strict";

/**
 * Cloud Functions for Expedite.
 *
 * drawLotteries: once a day, just after midnight Alberta time, finds lotteries whose entry
 * period has ended and draws their winners. See SETUP.txt for how to deploy.
 * notifyLotteryResults: when a lottery is drawn, pushes "You won" / "You weren't selected" to
 * everyone on its waiting list.
 * notifyOwnerMessage: when an owner sends a message to their waiting list (the app creates a
 * notifications/<id> document), pushes it to everyone on that list.
 * notifyIncomingCall: when someone starts a voice call from a chat (the app creates a
 * calls/<id> document, see voip/CallSignaling.kt), pushes "Incoming call" to the callee.
 *
 * Pushes go to the token the app saves in users/<id>.fcmToken (Database.saveOwnFcmToken), and
 * only to users whose notificationsEnabled isn't false. Message wording is in messages.js.
 *
 * Lottery documents are written by the Android app (see Database.kt, lotteryToMap). The fields
 * used here:
 *   users        string[]   entrants on the waiting list
 *   winnerCount  number     how many winners to draw
 *   entryEnd     Timestamp  when entries close (end of the owner's last entry day)
 *   drawn        boolean    false until this function draws it
 *   cancelled    boolean    cancelled lotteries are never drawn
 * Written here:
 *   winners      string[]   the drawn entrants
 *   drawn        true
 *   drawnAt      Timestamp  when the draw happened
 */

const { onSchedule } = require("firebase-functions/v2/scheduler");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");
const logger = require("firebase-functions/logger");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp, FieldValue } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { pickWinners } = require("./draw");
const {
  describeCampsites,
  resultMessages,
  ownerMessages,
  incomingCallMessage,
  chunk,
} = require("./messages");

initializeApp();
const db = getFirestore();
const messaging = getMessaging();

/** Lotteries fetched per query. The run keeps fetching until none are left to draw. */
const BATCH_SIZE = 100;

exports.drawLotteries = onSchedule(
  {
    // Entries close at midnight at the end of the owner's last entry day (see LotteryRules.entryWindow
    // in the app), so one run just after midnight Alberta time draws every lottery that closed.
    // Owners in other time zones have lotteries closing at other times; those are drawn at the
    // next run, up to a day later. If that ever matters, change this to "1 * * * *" (hourly).
    schedule: "1 0 * * *",
    timeZone: "America/Edmonton",
    // If a run fails (for example Firestore is briefly unavailable), try again a few minutes later
    // instead of waiting for the next night.
    retryCount: 3,
    minBackoffSeconds: 120,
    // One run at a time, so two runs never draw the same lottery. (The transaction below also
    // guards against that.)
    maxInstances: 1,
  },
  async () => {
    const now = Timestamp.now();
    // Lotteries that failed this run, so they aren't fetched again in a loop.
    const failed = new Set();
    let drawnCount = 0;

    for (;;) {
      // Needs the composite index in firestore.indexes.json (drawn, cancelled, entryEnd).
      const due = await db
        .collection("lotteries")
        .where("drawn", "==", false)
        .where("cancelled", "==", false)
        .where("entryEnd", "<=", now)
        .limit(BATCH_SIZE + failed.size)
        .get();

      const todo = due.docs.filter((doc) => !failed.has(doc.id));
      if (todo.length === 0) break;

      for (const doc of todo) {
        try {
          const winners = await drawLottery(doc.ref);
          if (winners !== null) {
            drawnCount++;
            logger.info(`Drew lottery ${doc.id}`, { winners: winners.length });
          } else {
            failed.add(doc.id); // skipped (cancelled meanwhile): don't fetch it again this run
          }
        } catch (error) {
          // One bad lottery shouldn't stop the others; it is tried again the next night.
          failed.add(doc.id);
          logger.error(`Couldn't draw lottery ${doc.id}`, error);
        }
      }
    }

    logger.info(`Draw run finished`, { drawn: drawnCount, failed: failed.size });
  },
);

/**
 * Draws one lottery inside a transaction. The transaction re-reads the lottery, so a lottery
 * that was cancelled (or already drawn) after the query ran is left alone, and entrants who
 * joined at the last second are included.
 *
 * @returns {Promise<string[] | null>} The winners, or null if it was skipped.
 */
async function drawLottery(ref) {
  return db.runTransaction(async (tx) => {
    const snap = await tx.get(ref);
    const data = snap.data();
    if (!data || data.drawn === true || data.cancelled === true) return null;
    if (data.entryEnd.toMillis() > Date.now()) return null;

    const winners = pickWinners(data.users ?? [], data.winnerCount ?? 0);
    tx.update(ref, {
      winners,
      drawn: true,
      drawnAt: FieldValue.serverTimestamp(),
    });
    return winners;
  });
}

// region Push notifications

/** FCM accepts at most this many messages per sendEach call. */
const MAX_PUSHES_PER_CALL = 500;

/** Documents read per getAll call, to keep each request a reasonable size. */
const READ_CHUNK = 100;

/**
 * FCM errors meaning the token will never work again (app uninstalled, data cleared, token
 * replaced). Only these clear the saved token: other errors, like a malformed message, are our
 * fault or temporary, and clearing tokens for them would silence everyone.
 */
const DEAD_TOKEN_ERRORS = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

exports.notifyLotteryResults = onDocumentUpdated("lotteries/{lotteryId}", async (event) => {
  const before = event.data?.before.data();
  const after = event.data?.after.data();
  // Runs on every change to any lottery (entrants joining, bookings...). Only the change that
  // marks it drawn matters; everything else returns straight away.
  if (!before || !after) return;
  if (before.drawn === true || after.drawn !== true || after.cancelled === true) return;

  const lotteryId = event.params.lotteryId;
  const recipients = await recipientsFor([...new Set(after.users ?? [])]);
  if (recipients.length === 0) {
    logger.info(`Lottery ${lotteryId} drawn; nobody to notify`);
    return;
  }

  const place = await placeName(after.campsites ?? []);
  const messages = resultMessages({ lotteryId, place, winners: after.winners ?? [], recipients });
  const result = await sendAll(messages, recipients);
  logger.info(`Sent results for lottery ${lotteryId}`, result);
});

exports.notifyOwnerMessage = onDocumentCreated("notifications/{notificationId}", async (event) => {
  // Fields written by Database.sendLotteryNotification in the app.
  const notification = event.data?.data();
  if (!notification) return;
  const { lottery: lotteryId, sender, title, text } = notification;
  if (typeof lotteryId !== "string" || typeof text !== "string" || text.trim() === "") {
    logger.warn(`Notification ${event.params.notificationId} has no lottery or text; not sent`);
    return;
  }

  const lottery = (await db.collection("lotteries").doc(lotteryId).get()).data();
  if (!lottery) {
    logger.warn(`Notification ${event.params.notificationId}: lottery ${lotteryId} not found`);
    return;
  }

  // Same people the app gave it to: the waiting list. The owner doesn't need their own message.
  const userIds = [...new Set(lottery.users ?? [])].filter((id) => id !== sender);
  const recipients = await recipientsFor(userIds);
  if (recipients.length === 0) return;

  const place = await placeName(lottery.campsites ?? []);
  const messages = ownerMessages({
    lotteryId,
    notificationId: event.params.notificationId,
    place,
    title,
    text,
    recipients,
  });
  const result = await sendAll(messages, recipients);
  logger.info(`Sent notification ${event.params.notificationId}`, result);
});

exports.notifyIncomingCall = onDocumentCreated("calls/{callId}", async (event) => {
  // Fields written by CallSignaling.createCall in the app.
  const call = event.data?.data();
  if (!call || call.status !== "ringing" || typeof call.calleeId !== "string") return;

  const [recipient] = await recipientsFor([call.calleeId]);
  if (!recipient) {
    logger.info(`Call ${event.params.callId}: callee can't be pushed; only rings if the app is open`);
    return;
  }

  const message = incomingCallMessage({
    callId: event.params.callId,
    callerName: call.callerName,
    video: call.video === true,
    token: recipient.token,
  });
  const result = await sendAll([message], [recipient]);
  logger.info(`Rang callee for call ${event.params.callId}`, result);
});

/**
 * The users among `userIds` who can be sent a push: they exist, haven't turned notifications
 * off, and have a saved token.
 *
 * @param {string[]} userIds Ids of users/<id> documents.
 * @returns {Promise<{ userId: string, token: string }[]>}
 */
async function recipientsFor(userIds) {
  const recipients = [];
  for (const ids of chunk(userIds, READ_CHUNK)) {
    const snaps = await db.getAll(...ids.map((id) => db.collection("users").doc(id)));
    for (const snap of snaps) {
      const user = snap.data();
      if (!user || user.notificationsEnabled === false) continue;
      if (typeof user.fcmToken !== "string" || user.fcmToken === "") continue;
      recipients.push({ userId: snap.id, token: user.fcmToken });
    }
  }
  return recipients;
}

/** What the lottery is for, in words, from its campsites' names. See describeCampsites. */
async function placeName(campsiteIds) {
  if (campsiteIds.length === 0) return describeCampsites([]);
  const snaps = await db.getAll(...campsiteIds.map((id) => db.collection("campsites").doc(id)));
  return describeCampsites(snaps.map((snap) => snap.get("name")));
}

/**
 * Sends `messages` (messages[i] goes to recipients[i]) and clears tokens FCM says are dead.
 * One failed device doesn't stop the others: sendEach reports each message separately.
 *
 * @returns {Promise<{ sent: number, failed: number, deadTokens: number }>}
 */
async function sendAll(messages, recipients) {
  let sent = 0;
  let failed = 0;
  const dead = [];

  for (let start = 0; start < messages.length; start += MAX_PUSHES_PER_CALL) {
    const response = await messaging.sendEach(messages.slice(start, start + MAX_PUSHES_PER_CALL));
    response.responses.forEach((r, i) => {
      if (r.success) {
        sent++;
        return;
      }
      failed++;
      const recipient = recipients[start + i];
      if (DEAD_TOKEN_ERRORS.has(r.error?.code)) {
        dead.push(recipient);
      } else {
        logger.warn(`Push to user ${recipient.userId} failed`, { code: r.error?.code });
      }
    });
  }

  await Promise.all(dead.map(forgetToken));
  return { sent, failed, deadTokens: dead.length };
}

/**
 * Removes a dead token from its user, unless the app has saved a new one since: the
 * transaction re-reads the user and only deletes the exact token that failed.
 */
async function forgetToken({ userId, token }) {
  const ref = db.collection("users").doc(userId);
  try {
    await db.runTransaction(async (tx) => {
      const snap = await tx.get(ref);
      if (snap.get("fcmToken") === token) tx.update(ref, { fcmToken: FieldValue.delete() });
    });
  } catch (error) {
    logger.warn(`Couldn't clear dead token for user ${userId}`, error);
  }
}

// endregion
