package com.KIRA_ZINA.backend.twentyfortyeight.domain;

import java.util.List;

public record Game2048State(
        String sessionId,
        int size,
        int score,
        boolean gameOver,
        boolean moved,
        int movesMade,
        List<Game2048Tile> tiles,
        int iceBlockCount
) {
}
