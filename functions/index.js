"use strict";

/**
 * Cloud Functions for Expedite.
 *
 * drawLotteries: once a day, just after midnight Alberta time, finds lotteries whose entry
 * period has ended and draws their winners. See SETUP.txt for how to deploy.
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
const logger = require("firebase-functions/logger");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, Timestamp, FieldValue } = require("firebase-admin/firestore");
const { pickWinners } = require("./draw");

initializeApp();
const db = getFirestore();

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
