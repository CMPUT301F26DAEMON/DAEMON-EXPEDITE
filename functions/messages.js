"use strict";

/**
 * Builds the push notifications the app receives. Kept free of Firebase, like draw.js, so it can
 * be tested without it. index.js does the reading and sending.
 *
 * Every push is an FCM "notification" message: when the app is in the background or closed,
 * Android shows it by itself, so it must carry the title, text and channel. When the app is open,
 * the app's ExpediteMessagingService.onMessageReceived shows it instead.
 */

/**
 * The Android notification channel every push goes in. Must match
 * NotificationChannels.LOTTERY_UPDATES in the app; a channel that doesn't exist on the phone
 * makes Android fall back to a default one without the pop-up card.
 */
const CHANNEL_ID = "lottery_updates";

/**
 * A recipient: one user and their device's push token (users/<id>.fcmToken).
 * @typedef {{ userId: string, token: string }} Recipient
 */

/**
 * Names the lottery in a sentence, by its campsites (lotteries don't have names of their own).
 *
 * @param {unknown[]} names Campsite names; blanks and non-strings are ignored.
 * @returns {string} e.g. "Pine Lake", "Pine Lake and 2 other campsites", or a generic phrase.
 */
function describeCampsites(names) {
  const clean = names.filter((n) => typeof n === "string" && n.trim() !== "").map((n) => n.trim());
  if (clean.length === 0) return "a campsite lottery";
  if (clean.length === 1) return clean[0];
  const others = clean.length - 1;
  return `${clean[0]} and ${others} other campsite${others === 1 ? "" : "s"}`;
}

/**
 * One FCM message for one device.
 *
 * @param {string} token The device's FCM token.
 * @param {string} title Bold first line of the notification.
 * @param {string} body The text under it.
 * @param {Record<string, string>} data Extra values the app can read (FCM requires strings).
 */
function pushMessage(token, title, body, data) {
  return {
    token,
    notification: { title, body },
    android: {
      // Delivered straight away, even if the phone is dozing. Normal priority can be held back
      // for minutes, which is wrong for something the user is waiting to hear.
      priority: "high",
      notification: { channelId: CHANNEL_ID },
    },
    // Not shown to the user. Says what the push is about, for when the app should open the
    // right screen on tap.
    data,
  };
}

/**
 * "You won" / "You weren't selected" pushes after a draw, one per recipient.
 *
 * @param {object} args
 * @param {string} args.lotteryId
 * @param {string} args.place What the lottery was for, from describeCampsites.
 * @param {string[]} args.winners User ids drawn as winners.
 * @param {Recipient[]} args.recipients Everyone on the waiting list who can be notified.
 * @returns {object[]} Messages in the same order as recipients.
 */
function resultMessages({ lotteryId, place, winners, recipients }) {
  const won = new Set(winners);
  return recipients.map(({ userId, token }) =>
    won.has(userId)
      ? pushMessage(
          token,
          "You won the lottery!",
          `You were selected for ${place}. Open Expedite to book your stay.`,
          { type: "lottery_result", lotteryId, result: "won" },
        )
      : pushMessage(
          token,
          "Lottery results",
          `You weren't selected for ${place} this time.`,
          { type: "lottery_result", lotteryId, result: "lost" },
        ),
  );
}

/**
 * Pushes for a message an owner sent to their lottery's waiting list, one per recipient.
 * The owner's title is optional in the app; without one, the heading says what it's about.
 *
 * @param {object} args
 * @param {string} args.lotteryId
 * @param {string} args.notificationId The notifications/<id> document it came from.
 * @param {string} args.place What the lottery is for, from describeCampsites.
 * @param {string | null | undefined} args.title The owner's heading, if any.
 * @param {string} args.text The owner's message.
 * @param {Recipient[]} args.recipients
 * @returns {object[]} Messages in the same order as recipients.
 */
function ownerMessages({ lotteryId, notificationId, place, title, text, recipients }) {
  const heading = (typeof title === "string" && title.trim()) || `Message about ${place}`;
  return recipients.map(({ token }) =>
    pushMessage(token, heading, text.trim(), { type: "owner_message", lotteryId, notificationId }),
  );
}

/**
 * The push that rings the callee when someone calls them from a chat. Tapping it opens the app,
 * which shows the incoming call (the app finds the ringing call in Firestore by itself).
 *
 * @param {object} args
 * @param {string} args.callId The calls/<id> document.
 * @param {string | null | undefined} args.callerName Who is calling.
 * @param {boolean} [args.video] True for a video call.
 * @param {string} args.token The callee's FCM token.
 * @returns {object} One message.
 */
function incomingCallMessage({ callId, callerName, video, token }) {
  const name = (typeof callerName === "string" && callerName.trim()) || "Someone";
  const kind = video === true ? "video call" : "call";
  const message = pushMessage(token, `Incoming ${kind}`, `${name} is calling you on Expedite.`, {
    type: "incoming_call",
    callId,
    video: String(video === true),
  });
  // A call is only worth answering for a short while; don't deliver it late.
  message.android.ttl = 45 * 1000;
  return message;
}

/** Splits `items` into arrays of at most `size`. */
function chunk(items, size) {
  const chunks = [];
  for (let i = 0; i < items.length; i += size) chunks.push(items.slice(i, i + size));
  return chunks;
}

module.exports = {
  CHANNEL_ID,
  describeCampsites,
  resultMessages,
  ownerMessages,
  incomingCallMessage,
  chunk,
};
