package com.KIRA_ZINA.backend.common;

import java.util.List;
import java.util.Map;

public record RoomStateResponse(
        String roomId,
        String gameType,
        String state,
        int playerCount,
        List<PlayerState> players
) {
    public record PlayerState(
            String playerId,
            String playerName,
            Map<String, Object> metrics
    ) {}
}
