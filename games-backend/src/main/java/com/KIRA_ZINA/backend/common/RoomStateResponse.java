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
        int totalPlayers
) {
    public record PlayerState(
            String playerId,
            String playerName,
            Map<String, Object> metrics
    ) {}
}
