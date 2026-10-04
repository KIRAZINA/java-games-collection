package com.KIRA_ZINA.backend.common;

import java.util.List;
import java.util.Map;

public record RoomStateResponse(
        String roomId,
        String gameType,
        String state,
        int playerCount,
        List<PlayerState> players,
        String roomPhase,
        long timeRemaining,
        long gameStartTime,
        boolean allPlayersReady,
        int readyCount,
        int totalPlayers,
        // Settled-result exposure (Step 6b.3): settleGame() computes both on
        // GameRoom but nothing observable carried them before this. Both stay
        // null until the room actually reaches GAME_OVER with a winner, so a
        // null winnerId means "not settled (or no score could be extracted)".
        String winnerId,
        Integer winnerScore
) {
    public record PlayerState(
            String playerId,
            String playerName,
            Map<String, Object> metrics
    ) {}
}
