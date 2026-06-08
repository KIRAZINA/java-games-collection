package com.KIRA_ZINA.backend.minesweeper.domain;

public record MinesweeperCellView(
        int row,
        int col,
        MinesweeperCellState state,
        int adjacentMines,
        boolean mine
) {
}
