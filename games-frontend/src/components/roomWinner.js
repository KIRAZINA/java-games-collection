// Shared, pure view helper for the room-based games (2048, Minesweeper): turns
// a GET /api/rooms/{roomId}/state payload into the label shown once the room
// settles. Returns null until the room has really reached GAME_OVER with a
// winner, so callers render the winner line conditionally. Kept in one place so
// the two games cannot drift apart on the exact wording the e2e pins.
export function roomWinnerLabel(roomState) {
  if (!roomState || roomState.roomPhase !== 'GAME_OVER') return null;
  if (!roomState.winnerId) return null;
  const winner = (roomState.players ?? []).find((p) => p.playerId === roomState.winnerId);
  const name = winner?.playerName || roomState.winnerId;
  const score = roomState.winnerScore;
  return score === null || score === undefined ? name : `${name} (${score})`;
}
