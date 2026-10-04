import { describe, it, expect } from 'vitest';
import { roomWinnerLabel } from '../components/roomWinner.js';

// Step 6b.3: the settled result reaches the UI as "Winner: <name> (<score>)".
// The e2e pins that exact wording, so the helper is unit-tested on every branch.
describe('roomWinnerLabel', () => {
  const settled = {
    roomPhase: 'GAME_OVER',
    winnerId: 'p-1',
    winnerScore: 4242,
    players: [{ playerId: 'p-1', playerName: 'Alice', metrics: {} }],
  };

  it('returns null before the room settles', () => {
    expect(
      roomWinnerLabel({ roomPhase: 'PLAYING', winnerId: null, winnerScore: null, players: [] })
    ).toBeNull();
  });

  it('returns null for a settled room with no extractable winner', () => {
    expect(roomWinnerLabel({ ...settled, winnerId: null, winnerScore: null })).toBeNull();
  });

  it('returns null for an empty payload', () => {
    expect(roomWinnerLabel(null)).toBeNull();
  });

  it('names the winner with the score when the player is in the payload', () => {
    expect(roomWinnerLabel(settled)).toBe('Alice (4242)');
  });

  it('falls back to the raw playerId when no player name is available', () => {
    expect(roomWinnerLabel({ ...settled, players: [] })).toBe('p-1 (4242)');
  });

  it('omits the score when none was reported', () => {
    expect(roomWinnerLabel({ ...settled, winnerScore: null })).toBe('Alice');
  });
});
