"use strict";

const test = require("node:test");
const assert = require("node:assert/strict");
const { pickWinners } = require("../draw");

test("picks the requested number of different winners from the entrants", () => {
  const entrants = ["a", "b", "c", "d", "e"];
  const winners = pickWinners(entrants, 3);
  assert.equal(winners.length, 3);
  assert.equal(new Set(winners).size, 3);
  for (const w of winners) assert.ok(entrants.includes(w));
});

test("everyone wins when fewer joined than there are winners", () => {
  assert.deepEqual(pickWinners(["a", "b"], 5).sort(), ["a", "b"]);
});

test("no entrants or no winners means nobody wins", () => {
  assert.deepEqual(pickWinners([], 3), []);
  assert.deepEqual(pickWinners(["a"], 0), []);
});

test("duplicate entrants are only counted once", () => {
  assert.deepEqual(pickWinners(["a", "a", "a"], 2), ["a"]);
});

test("uses the random source to choose", () => {
  // Always picking the last remaining entrant: [a,b,c] -> swap a,c -> [c,b,a] -> swap b,a -> [c,a,b].
  const last = (max) => max - 1;
  assert.deepEqual(pickWinners(["a", "b", "c"], 2, last), ["c", "a"]);
  // Always picking the first keeps the original order.
  assert.deepEqual(pickWinners(["a", "b", "c"], 2, () => 0), ["a", "b"]);
});

test("every entrant can win (roughly even over many draws)", () => {
  const counts = { a: 0, b: 0, c: 0, d: 0 };
  const runs = 4000;
  for (let i = 0; i < runs; i++) counts[pickWinners(["a", "b", "c", "d"], 1)[0]]++;
  for (const n of Object.values(counts)) {
    // Expected 1000 each; allow a wide margin so the test never flakes.
    assert.ok(n > 800 && n < 1200, `counts were ${JSON.stringify(counts)}`);
  }
});
