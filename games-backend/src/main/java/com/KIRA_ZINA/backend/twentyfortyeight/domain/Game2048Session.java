package com.KIRA_ZINA.backend.twentyfortyeight.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Game2048Session {
    public static final int SIZE = 4;
    private static final long ICE_BLOCK_INTERVAL_MS = 15_000;

    private final String id;
    private final int[] cells = new int[SIZE * SIZE];
    private int score;
    private boolean gameOver;
    private boolean lastMoveChanged;
    private int movesMade;
    private int iceBlockCount;
    private long lastIceBlockTime;
    private Instant lastTouched = Instant.now();

    public Game2048Session(String id) {
        this.id = id;
        reset();
    }

    public String getSessionId() {
        return id;
    }

    public synchronized Game2048State move(MoveDirection direction) {
        if (gameOver) {
            touch();
            return snapshot();
        }

        lastMoveChanged = switch (direction) {
            case UP -> moveVertical(true);
            case DOWN -> moveVertical(false);
            case LEFT -> moveHorizontal(true);
            case RIGHT -> moveHorizontal(false);
        };

        if (lastMoveChanged) {
            addRandomTile();
            movesMade++;
        }

        injectIceBlocks();
        gameOver = !hasAvailableMove();

        touch();
        return snapshot();
    }

    public synchronized Game2048State reset() {
        for (int i = 0; i < cells.length; i++) {
            cells[i] = 0;
        }
        score = 0;
        gameOver = false;
        lastMoveChanged = false;
        movesMade = 0;
        iceBlockCount = 0;
        lastIceBlockTime = 0;
        addRandomTile();
        addRandomTile();
        touch();
        return snapshot();
    }

    public synchronized Game2048State state() {
        touch();
        return snapshot();
    }

    public synchronized void close() {
        for (int i = 0; i < cells.length; i++) {
            cells[i] = 0;
        }
        gameOver = true;
        touch();
    }

    public synchronized Instant lastTouched() {
        return lastTouched;
    }

    private void injectIceBlocks() {
        long now = System.currentTimeMillis();
        if (lastIceBlockTime == 0) {
            lastIceBlockTime = now;
            return;
        }
        while (now - lastIceBlockTime >= ICE_BLOCK_INTERVAL_MS) {
            addIceBlock();
            lastIceBlockTime += ICE_BLOCK_INTERVAL_MS;
        }
    }

    private void addIceBlock() {
        List<Integer> emptyCells = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == 0) emptyCells.add(i);
        }
        if (emptyCells.isEmpty()) return;
        int idx = emptyCells.get(ThreadLocalRandom.current().nextInt(emptyCells.size()));
        cells[idx] = -1;
        iceBlockCount++;
    }

    private boolean moveHorizontal(boolean left) {
        boolean changed = false;
        for (int row = 0; row < SIZE; row++) {
            int[] line = new int[SIZE];
            for (int offset = 0; offset < SIZE; offset++) {
                int col = left ? offset : SIZE - 1 - offset;
                line[offset] = cells[index(row, col)];
            }
            int[] result = mergeLineWithObstacles(line);
            for (int offset = 0; offset < SIZE; offset++) {
                int col = left ? offset : SIZE - 1 - offset;
                int targetIndex = index(row, col);
                if (cells[targetIndex] != result[offset]) {
                    cells[targetIndex] = result[offset];
                    changed = true;
                }
            }
        }
        return changed;
    }

    private boolean moveVertical(boolean up) {
        boolean changed = false;
        for (int col = 0; col < SIZE; col++) {
            int[] line = new int[SIZE];
            for (int offset = 0; offset < SIZE; offset++) {
                int row = up ? offset : SIZE - 1 - offset;
                line[offset] = cells[index(row, col)];
            }
            int[] result = mergeLineWithObstacles(line);
            for (int offset = 0; offset < SIZE; offset++) {
                int row = up ? offset : SIZE - 1 - offset;
                int targetIndex = index(row, col);
                if (cells[targetIndex] != result[offset]) {
                    cells[targetIndex] = result[offset];
                    changed = true;
                }
            }
        }
        return changed;
    }

    private int[] mergeLineWithObstacles(int[] line) {
        boolean[] ice = new boolean[SIZE];
        for (int i = 0; i < SIZE; i++) {
            if (line[i] == -1) ice[i] = true;
        }

        int[] movable = new int[SIZE];
        int movableCount = 0;
        for (int i = 0; i < SIZE; i++) {
            if (line[i] > 0) {
                movable[movableCount++] = line[i];
            }
        }

        int[] merged = new int[movableCount];
        boolean[] mergedFlag = new boolean[movableCount];
        int writeIndex = 0;
        int scoreDelta = 0;
        for (int readIndex = 0; readIndex < movableCount; readIndex++) {
            if (writeIndex > 0 && merged[writeIndex - 1] == movable[readIndex] && !mergedFlag[writeIndex - 1]) {
                merged[writeIndex - 1] *= 2;
                mergedFlag[writeIndex - 1] = true;
                scoreDelta += merged[writeIndex - 1];
            } else {
                merged[writeIndex++] = movable[readIndex];
            }
        }

        int[] result = new int[SIZE];
        int mIdx = 0;
        for (int i = 0; i < SIZE; i++) {
            if (ice[i]) {
                result[i] = -1;
            } else if (mIdx < writeIndex) {
                result[i] = merged[mIdx++];
            } else {
                result[i] = 0;
            }
        }

        score += scoreDelta;
        return result;
    }

    private boolean addRandomTile() {
        List<Integer> emptyCells = new ArrayList<>();
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == 0) emptyCells.add(i);
        }
        if (emptyCells.isEmpty()) return false;

        int idx = emptyCells.get(ThreadLocalRandom.current().nextInt(emptyCells.size()));
        cells[idx] = ThreadLocalRandom.current().nextInt(10) == 0 ? 4 : 2;
        return true;
    }

    private boolean hasAvailableMove() {
        for (int value : cells) {
            if (value == 0) return true;
        }
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int value = cells[index(row, col)];
                if (value <= 0) continue;
                if (col + 1 < SIZE && cells[index(row, col + 1)] == value) return true;
                if (row + 1 < SIZE && cells[index(row + 1, col)] == value) return true;
            }
        }
        return false;
    }

    private Game2048State snapshot() {
        List<Game2048Tile> tiles = new ArrayList<>(SIZE * SIZE);
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int value = cells[index(row, col)];
                if (value != 0) {
                    tiles.add(new Game2048Tile(row, col, value));
                }
            }
        }
        return new Game2048State(id, SIZE, score, gameOver, lastMoveChanged, movesMade, tiles, iceBlockCount);
    }

    private int index(int row, int col) {
        return row * SIZE + col;
    }

    private void touch() {
        lastTouched = Instant.now();
    }
}
