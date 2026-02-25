package com.KIRA_ZINA.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Core game logic model. Handles grid generation, mine placement,
 * flood fill recursion, and win/loss states.
 */
public class MinesweeperModel {
    private final int rows;
    private final int cols;
    private final int totalMines;
    private final Cell[][] grid;

    private boolean gameOver;
    private boolean won;
    private boolean firstClickDone;
    private int flagsPlaced;
    private final Random random;

    // Listeners to notify UI about changes
    private List<GameListener> listeners = new ArrayList<>();

    public interface GameListener {
        void onGameStateChanged(boolean won, boolean lost);
        void onCellUpdated(int r, int c);
        void onFullRefresh();


        default void onTimerTick() {}
    }

    public MinesweeperModel(int rows, int cols, int mines) {
        this.rows = rows;
        this.cols = cols;
        this.totalMines = mines;
        this.grid = new Cell[rows][cols];
        this.gameOver = false;
        this.won = false;
        this.firstClickDone = false;
        this.flagsPlaced = 0;
        this.random = new Random();
        initializeGrid();
    }

    private void initializeGrid() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                grid[r][c] = new Cell(r, c);
            }
        }
    }

    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public int getTotalMines() { return totalMines; }
    public int getFlagsPlaced() { return flagsPlaced; }
    public int getRemainingMines() { return totalMines - flagsPlaced; }
    public boolean isGameOver() { return gameOver; }
    public boolean isWon() { return won; }
    public Cell getCell(int r, int c) { return grid[r][c]; }

    public void addListener(GameListener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(GameListener listener) {
        listeners.remove(listener);
    }

    private void notifyCellUpdated(int r, int c) {
        for (GameListener l : listeners) l.onCellUpdated(r, c);
    }

    private void notifyFullRefresh() {
        for (GameListener l : listeners) l.onFullRefresh();
    }

    private void notifyGameState(boolean won, boolean lost) {
        for (GameListener l : listeners) l.onGameStateChanged(won, lost);
    }

    /**
     * Places mines randomly, ensuring the safeRow/safeCol and its neighbors are not mines.
     * This creates a 3x3 safe zone around the first click (classic Minesweeper behavior).
     * Called on the first valid click.
     */
    private void placeMines(int safeRow, int safeCol) {
        List<Cell> outsideSafeZone = new ArrayList<>();
        List<Cell> safeZoneNeighbors = new ArrayList<>();
        Cell firstClickCell = grid[safeRow][safeCol];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (r == safeRow && c == safeCol) continue;
                
                boolean isNeighbor = Math.abs(r - safeRow) <= 1 && Math.abs(c - safeCol) <= 1;
                if (isNeighbor) {
                    safeZoneNeighbors.add(grid[r][c]);
                } else {
                    outsideSafeZone.add(grid[r][c]);
                }
            }
        }

        // Shuffle candidates
        java.util.Collections.shuffle(outsideSafeZone, random);
        java.util.Collections.shuffle(safeZoneNeighbors, random);

        int minesPlaced = 0;
        
        // 1. Fill from outside the safe zone first
        for (Cell cell : outsideSafeZone) {
            if (minesPlaced >= totalMines) break;
            cell.setMine(true);
            minesPlaced++;
        }

        // 2. If we still need more mines, fill from safe zone neighbors
        for (Cell cell : safeZoneNeighbors) {
            if (minesPlaced >= totalMines) break;
            cell.setMine(true);
            minesPlaced++;
        }

        // 3. If we STILL need more mines (extremely rare case where totalMines == rows * cols),
        // we have to put one in the first click cell
        if (minesPlaced < totalMines) {
            firstClickCell.setMine(true);
            minesPlaced++;
        }

        calculateAdjacentMines();
    }

    private void calculateAdjacentMines() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].isMine()) continue;
                int count = 0;
                for (int dr = -1; dr <= 1; dr++) {
                    for (int dc = -1; dc <= 1; dc++) {
                        int nr = r + dr;
                        int nc = c + dc;
                        if (isValid(nr, nc) && grid[nr][nc].isMine()) {
                            count++;
                        }
                    }
                }
                grid[r][c].setAdjacentMinesCount(count);
            }
        }
    }

    public boolean isValid(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    /**
     * Handles left-click logic.
     */
    public boolean openCell(int r, int c) {
        if (gameOver || !isValid(r, c)) return false;
        Cell cell = grid[r][c];

        if (cell.isFlagged() || cell.isOpened()) return false;

        // Safe First Move Logic
        if (!firstClickDone) {
            placeMines(r, c);
            firstClickDone = true;
        }

        if (cell.isMine()) {
            cell.setState(Cell.State.OPENED);
            notifyCellUpdated(r, c);
            loseGame(r, c);
            return true;
        }

        revealCell(r, c);
        checkWin();
        return true;
    }

    /**
     * Recursive Flood Fill to open empty areas.
     */
    private void revealCell(int r, int c) {
        if (!isValid(r, c)) return;
        Cell cell = grid[r][c];

        if (cell.isOpened() || cell.isFlagged()) return;

        cell.setState(Cell.State.OPENED);
        notifyCellUpdated(r, c);

        // If cell has no adjacent mines, open neighbors recursively
        if (cell.getAdjacentMinesCount() == 0) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    revealCell(r + dr, c + dc);
                }
            }
        }
    }

    /**
     * Handles right-click logic (Flagging).
     */
    public void toggleFlag(int r, int c) {
        if (gameOver || !isValid(r, c)) return;
        Cell cell = grid[r][c];

        if (cell.isOpened()) return;

        if (cell.isFlagged()) {
            cell.setState(Cell.State.COVERED);
            flagsPlaced--;
        } else {
            cell.setState(Cell.State.FLAGGED);
            flagsPlaced++;
        }
        notifyCellUpdated(r, c);
    }

    private void loseGame(int explodedR, int explodedC) {
        gameOver = true;
        won = false;
        // Reveal all mines and mark wrong flags
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = grid[r][c];
                if (cell.isMine()) {
                    cell.setState(Cell.State.OPENED);
                } else if (cell.isFlagged()) {
                    // Mark incorrect flags with WRONG_FLAG state
                    cell.setState(Cell.State.WRONG_FLAG);
                }
            }
        }
        notifyFullRefresh();
        notifyGameState(false, true);
    }

    private void checkWin() {
        if (gameOver) return;
        int openedCount = 0;
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                if (grid[r][c].isOpened()) openedCount++;
            }
        }
        // Win if all non-mine cells are opened
        if (openedCount == (rows * cols - totalMines)) {
            gameOver = true;
            won = true;
            // Update flags counter to match total mines
            flagsPlaced = totalMines;
            // Flag all mines visually
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    if (grid[r][c].isMine()) {
                        grid[r][c].setState(Cell.State.FLAGGED);
                    }
                }
            }
            notifyFullRefresh();
            notifyGameState(true, false);
        }
    }

    public void reset() {
        gameOver = false;
        won = false;
        firstClickDone = false;
        flagsPlaced = 0;
        initializeGrid();
        notifyFullRefresh();
    }

    public boolean isFirstClickDone() {
        return firstClickDone;
    }
}
