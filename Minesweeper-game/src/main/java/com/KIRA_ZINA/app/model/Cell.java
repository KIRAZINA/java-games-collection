package com.KIRA_ZINA.app.model;

/**
 * Represents a single cell in the Minesweeper grid.
 * Contains logic for state (covered, flagged, opened) and data (isMine, adjacent mines).
 */
public class Cell {
    public enum State {
        COVERED,
        OPENED,
        FLAGGED,
        WRONG_FLAG  // Flag placed on non-mine cell (shown on game over)
    }

    private final int row;
    private final int col;
    private boolean isMine;
    private int adjacentMinesCount;
    private State state;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.isMine = false;
        this.adjacentMinesCount = 0;
        this.state = State.COVERED;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
    public boolean isMine() { return isMine; }
    public void setMine(boolean mine) { isMine = mine; }
    public int getAdjacentMinesCount() { return adjacentMinesCount; }
    public void setAdjacentMinesCount(int count) { adjacentMinesCount = count; }
    public State getState() { return state; }

    public void setState(State state) {
        this.state = state;
    }

    public boolean isCovered() {
        return state == State.COVERED;
    }

    public boolean isFlagged() {
        return state == State.FLAGGED;
    }

    public boolean isOpened() {
        return state == State.OPENED;
    }

    public boolean isWrongFlag() {
        return state == State.WRONG_FLAG;
    }
}
