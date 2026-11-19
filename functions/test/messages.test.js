"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const { CHANNEL_ID, describeCampsites, resultMessages, ownerMessages, chunk } = require("../messages");

const recipients = [
  { userId: "u1", token: "t1" },
  { userId: "u2", token: "t2" },
  { userId: "u3", token: "t3" },
];

test("names the lottery by its campsites", () => {
  assert.equal(describeCampsites(["Pine Lake"]), "Pine Lake");
  assert.equal(describeCampsites(["Pine Lake", "Bow River"]), "Pine Lake and 1 other campsite");
  assert.equal(describeCampsites(["Pine Lake", "Bow River", "Elk"]), "Pine Lake and 2 other campsites");
});

test("missing or blank campsite names fall back to a generic phrase", () => {
  assert.equal(describeCampsites([]), "a campsite lottery");
  assert.equal(describeCampsites([undefined, "  ", null]), "a campsite lottery");
  assert.equal(describeCampsites([undefined, " Pine Lake "]), "Pine Lake");
});

test("winners are told they won and everyone else that they weren't selected", () => {
  const messages = resultMessages({ lotteryId: "L", place: "Pine Lake", winners: ["u2"], recipients });
  assert.deepEqual(
    messages.map((m) => [m.token, m.data.result]),
    [["t1", "lost"], ["t2", "won"], ["t3", "lost"]],
  );
  assert.match(messages[1].notification.body, /selected for Pine Lake/);
  assert.match(messages[0].notification.body, /weren't selected for Pine Lake/);
});

test("every push is high priority, in the app's channel, with string data", () => {
  const all = [
    ...resultMessages({ lotteryId: "L", place: "P", winners: [], recipients }),
    ...ownerMessages({ lotteryId: "L", notificationId: "N", place: "P", title: null, text: "hi", recipients }),
  ];
  for (const m of all) {
    assert.equal(m.android.priority, "high");
    assert.equal(m.android.notification.channelId, CHANNEL_ID);
    // FCM rejects data values that aren't strings.
    for (const v of Object.values(m.data)) assert.equal(typeof v, "string");
  }
  assert.equal(CHANNEL_ID, "lottery_updates");
});

test("owner messages use the owner's title, or say what they're about", () => {
  const titled = ownerMessages({
    lotteryId: "L", notificationId: "N", place: "Pine Lake", title: " Gate code ", text: " 1234 ", recipients,
  });
  assert.equal(titled[0].notification.title, "Gate code");
  assert.equal(titled[0].notification.body, "1234");
  assert.deepEqual(titled[0].data, { type: "owner_message", lotteryId: "L", notificationId: "N" });

  for (const title of [null, undefined, "   "]) {
    const untitled = ownerMessages({ lotteryId: "L", notificationId: "N", place: "Pine Lake", title, text: "hi", recipients });
    assert.equal(untitled[0].notification.title, "Message about Pine Lake");
  }
});

test("chunk splits into pieces of at most the given size", () => {
  assert.deepEqual(chunk([1, 2, 3, 4, 5], 2), [[1, 2], [3, 4], [5]]);
  assert.deepEqual(chunk([], 2), []);
});
