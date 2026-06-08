package com.KIRA_ZINA.backend.common;

import java.util.Map;

public record GameSettings(
        GameType gameType,
        Map<String, Object> settings,
        boolean passwordProtected,
        String passwordHash,
        boolean allowBots,
        int maxPlayers
) {
    public static GameSettings defaultFor(GameType gameType) {
        return switch (gameType) {
            case BLACKJACK -> new GameSettings(
                    GameType.BLACKJACK,
                    Map.of("initialBalance", 100.0, "difficulty", "BASIC"),
                    false, null, true, 4
            );
            case MINESWEEPER -> new GameSettings(
                    GameType.MINESWEEPER,
                    Map.of("rows", 9, "cols", 9, "mines", 10),
                    false, null, false, 1
            );
            case TWENTY_FORTY_EIGHT -> new GameSettings(
                    GameType.TWENTY_FORTY_EIGHT,
                    Map.of("size", 4),
                    false, null, false, 1
            );
        };
    }
}