package com.KIRA_ZINA.backend.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import java.util.Map;

public record GameSettings(
        GameType gameType,
        Map<String, Object> settings,
        boolean passwordProtected,
        String passwordHash,
        boolean allowBots,
        @Min(value = 1, message = "must be greater than or equal to 1") @Max(value = 8, message = "must be less than or equal to 8") int maxPlayers,
        int timeLimitSeconds,
        boolean isSinglePlayer
) {
    public static GameSettings defaultFor(GameType gameType) {
        return switch (gameType) {
            case BLACKJACK -> new GameSettings(
                    GameType.BLACKJACK,
                    Map.of("initialBalance", 100.0, "difficulty", "BASIC"),
                    false, null, true, 4, 0, false
            );
            case MINESWEEPER -> new GameSettings(
                    GameType.MINESWEEPER,
                    Map.of("rows", 9, "cols", 9, "mines", 10),
                    false, null, false, 2, 60, false
            );
            case TWENTY_FORTY_EIGHT -> new GameSettings(
                    GameType.TWENTY_FORTY_EIGHT,
                    Map.of("size", 4),
                    false, null, false, 2, 60, false
            );
        };
    }
}
