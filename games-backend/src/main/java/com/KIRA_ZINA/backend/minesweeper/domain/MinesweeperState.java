package com.KIRA_ZINA.backend.minesweeper.domain;

import java.util.List;

public record MinesweeperState(
        String sessionId,
        int rows,
        int cols,
        int totalMines,
        int flagsPlaced,
        int remainingMines,
        boolean firstClickDone,
        boolean gameOver,
        boolean won,
        List<MinesweeperCellView> cells
) {
}
