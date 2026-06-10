package com.KIRA_ZINA.backend.common;

import java.util.List;

public record RoomProgressResponse(
        String roomId,
        String gameType,
        List<PlayerProgress> players,
        String roomPhase,
        long timeRemaining
) {
    public record PlayerProgress(
            String playerId,
            String playerName,
            int score,
            int boardsCleared,
            boolean gameOver,
            boolean won,
            int movesMade,
            double balance,
            String phase,
            int clearedFields,
            int flagsPlaced,
            boolean isLocked
    ) {}
}
