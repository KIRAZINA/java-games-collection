package com.KIRA_ZINA.backend.minesweeper.domain;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class MinesweeperSession {
    private final String id;
    private final int rows;
    private final int cols;
    private final int totalMines;
    private final BitSet mines;
    private final byte[] adjacentMineCounts;
    private final MinesweeperCellState[] states;
    private boolean gameOver;
    private boolean won;
    private boolean firstClickDone;
    private int flagsPlaced;
    private int openedCells;
    private Instant lastTouched = Instant.now();

    public MinesweeperSession(String id, int rows, int cols, int totalMines) {
        validateBoard(rows, cols, totalMines);
        this.id = id;
        this.rows = rows;
        this.cols = cols;
        this.totalMines = totalMines;
        int cellCount = rows * cols;
        this.mines = new BitSet(cellCount);
        this.adjacentMineCounts = new byte[cellCount];
        this.states = new MinesweeperCellState[cellCount];
        resetState();
    }

    public String getSessionId() {
        return id;
    }

    public synchronized MinesweeperState open(int row, int col) {
        if (gameOver || !isValid(row, col)) {
            touch();
            return snapshot();
        }

        int index = index(row, col);
        if (states[index] == MinesweeperCellState.FLAGGED || states[index] == MinesweeperCellState.OPENED) {
            touch();
            return snapshot();
        }

        if (!firstClickDone) {
            placeMines(row, col);
            firstClickDone = true;
        }

        if (mines.get(index)) {
            states[index] = MinesweeperCellState.OPENED;
            lose();
        } else {
            revealFrom(index);
            checkWin();
        }

        touch();
        return snapshot();
    }

    public synchronized MinesweeperState toggleFlag(int row, int col) {
        if (gameOver || !isValid(row, col)) {
            touch();
            return snapshot();
        }

        int index = index(row, col);
        if (states[index] == MinesweeperCellState.OPENED) {
            touch();
            return snapshot();
        }

        if (states[index] == MinesweeperCellState.FLAGGED) {
            states[index] = MinesweeperCellState.COVERED;
            flagsPlaced--;
        } else {
            states[index] = MinesweeperCellState.FLAGGED;
            flagsPlaced++;
        }

        touch();
        return snapshot();
    }

    public synchronized MinesweeperState reset() {
        resetState();
        touch();
        return snapshot();
    }

    public synchronized MinesweeperState state() {
        touch();
        return snapshot();
    }

    public synchronized void close() {
        mines.clear();
        gameOver = true;
        touch();
    }

    public synchronized Instant lastTouched() {
        return lastTouched;
    }

    private void resetState() {
        mines.clear();
        for (int i = 0; i < adjacentMineCounts.length; i++) {
            adjacentMineCounts[i] = 0;
            states[i] = MinesweeperCellState.COVERED;
        }
        gameOver = false;
        won = false;
        firstClickDone = false;
        flagsPlaced = 0;
        openedCells = 0;
    }

    private void placeMines(int safeRow, int safeCol) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int safeIndex = index(safeRow, safeCol);
        int placed = 0;

        while (placed < totalMines) {
            int candidate = random.nextInt(rows * cols);
            if (candidate == safeIndex || mines.get(candidate) || isSafeNeighbor(candidate, safeRow, safeCol)) {
                continue;
            }
            mines.set(candidate);
            placed++;
        }

        calculateAdjacentMines();
    }

    private boolean isSafeNeighbor(int candidate, int safeRow, int safeCol) {
        int row = candidate / cols;
        int col = candidate % cols;
        return Math.abs(row - safeRow) <= 1 && Math.abs(col - safeCol) <= 1
                && totalMines <= rows * cols - safeZoneSize(safeRow, safeCol);
    }

    private int safeZoneSize(int safeRow, int safeCol) {
        int count = 0;
        for (int row = safeRow - 1; row <= safeRow + 1; row++) {
            for (int col = safeCol - 1; col <= safeCol + 1; col++) {
                if (isValid(row, col)) {
                    count++;
                }
            }
        }
        return count;
    }

    private void calculateAdjacentMines() {
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = index(row, col);
                if (mines.get(index)) {
                    continue;
                }
                adjacentMineCounts[index] = (byte) countAdjacentMines(row, col);
            }
        }
    }

    private int countAdjacentMines(int row, int col) {
        int count = 0;
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) {
                    continue;
                }
                int neighborRow = row + dr;
                int neighborCol = col + dc;
                if (isValid(neighborRow, neighborCol) && mines.get(index(neighborRow, neighborCol))) {
                    count++;
                }
            }
        }
        return count;
    }

    private void revealFrom(int startIndex) {
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(startIndex);

        while (!queue.isEmpty()) {
            int current = queue.removeFirst();
            if (states[current] == MinesweeperCellState.OPENED || states[current] == MinesweeperCellState.FLAGGED) {
                continue;
            }

            states[current] = MinesweeperCellState.OPENED;
            openedCells++;

            if (adjacentMineCounts[current] != 0) {
                continue;
            }

            int row = current / cols;
            int col = current % cols;
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) {
                        continue;
                    }
                    int neighborRow = row + dr;
                    int neighborCol = col + dc;
                    if (isValid(neighborRow, neighborCol)) {
                        int neighbor = index(neighborRow, neighborCol);
                        if (!mines.get(neighbor) && states[neighbor] != MinesweeperCellState.OPENED) {
                            queue.add(neighbor);
                        }
                    }
                }
            }
        }
    }

    private void lose() {
        gameOver = true;
        won = false;
        for (int i = 0; i < states.length; i++) {
            if (mines.get(i)) {
                states[i] = MinesweeperCellState.OPENED;
            } else if (states[i] == MinesweeperCellState.FLAGGED) {
                states[i] = MinesweeperCellState.WRONG_FLAG;
            }
        }
    }

    private void checkWin() {
        if (openedCells != rows * cols - totalMines) {
            return;
        }
        gameOver = true;
        won = true;
        flagsPlaced = totalMines;
        for (int i = mines.nextSetBit(0); i >= 0; i = mines.nextSetBit(i + 1)) {
            states[i] = MinesweeperCellState.FLAGGED;
        }
    }

    private MinesweeperState snapshot() {
        List<MinesweeperCellView> cells = new ArrayList<>(states.length);
        boolean revealMines = gameOver;
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                int index = index(row, col);
                boolean opened = states[index] == MinesweeperCellState.OPENED;
                cells.add(new MinesweeperCellView(
                        row,
                        col,
                        states[index],
                        opened ? adjacentMineCounts[index] : 0,
                        revealMines && mines.get(index)
                ));
            }
        }
        return new MinesweeperState(
                id,
                rows,
                cols,
                totalMines,
                flagsPlaced,
                totalMines - flagsPlaced,
                firstClickDone,
                gameOver,
                won,
                cells
        );
    }

    private int index(int row, int col) {
        return row * cols + col;
    }

    private boolean isValid(int row, int col) {
        return row >= 0 && row < rows && col >= 0 && col < cols;
    }

    private void touch() {
        lastTouched = Instant.now();
    }

    private static void validateBoard(int rows, int cols, int totalMines) {
        if (rows < 4 || cols < 4 || rows > 30 || cols > 30) {
            throw new IllegalArgumentException("Rows and columns must be between 4 and 30");
        }
        int cellCount = rows * cols;
        if (totalMines < 1 || totalMines >= cellCount) {
            throw new IllegalArgumentException("Mine count must be between 1 and one less than the cell count");
        }
    }
}
