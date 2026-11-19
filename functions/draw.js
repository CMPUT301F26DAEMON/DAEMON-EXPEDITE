"use strict";

const { randomInt } = require("node:crypto");

/**
 * Picks up to `count` different winners from `entrants`, uniformly at random.
 *
 * Uses a partial Fisher–Yates shuffle with a cryptographically secure random source, so every
 * entrant has the same chance and the result can't be predicted. If fewer entrants joined than
 * there are winners, everyone wins. Duplicate ids are only counted once.
 *
 * @param {string[]} entrants User ids on the waiting list.
 * @param {number} count How many winners to draw.
 * @param {(max: number) => number} [random] Returns a whole number from 0 up to (not including)
 *   `max`. Defaults to crypto.randomInt; tests pass a fixed one.
 * @returns {string[]} The winners, in the order drawn.
 */
function pickWinners(entrants, count, random = randomInt) {
  const pool = [...new Set(entrants)];
  const winners = Math.max(0, Math.min(count, pool.length));
  for (let i = 0; i < winners; i++) {
    // Swap a random not-yet-picked entrant into position i.
    const j = i + random(pool.length - i);
    [pool[i], pool[j]] = [pool[j], pool[i]];
  }
  return pool.slice(0, winners);
}

module.exports = { pickWinners };
