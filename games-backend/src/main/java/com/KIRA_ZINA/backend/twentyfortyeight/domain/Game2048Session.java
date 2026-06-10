package com.KIRA_ZINA.backend.twentyfortyeight.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class Game2048Session {
    public static final int SIZE = 4;

    private final String id;
    private final int[] cells = new int[SIZE * SIZE];
    private int score;
    private boolean gameOver;
    private boolean lastMoveChanged;
    private int movesMade;
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

    private boolean moveHorizontal(boolean left) {
        boolean changed = false;
        for (int row = 0; row < SIZE; row++) {
            int[] line = new int[SIZE];
            for (int offset = 0; offset < SIZE; offset++) {
                int col = left ? offset : SIZE - 1 - offset;
                line[offset] = cells[index(row, col)];
            }
            MoveResult result = mergeLine(line);
            for (int offset = 0; offset < SIZE; offset++) {
                int col = left ? offset : SIZE - 1 - offset;
                int targetIndex = index(row, col);
                if (cells[targetIndex] != result.values[offset]) {
                    cells[targetIndex] = result.values[offset];
                    changed = true;
                }
            }
            score += result.scoreDelta;
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
            MoveResult result = mergeLine(line);
            for (int offset = 0; offset < SIZE; offset++) {
                int row = up ? offset : SIZE - 1 - offset;
                int targetIndex = index(row, col);
                if (cells[targetIndex] != result.values[offset]) {
                    cells[targetIndex] = result.values[offset];
                    changed = true;
                }
            }
            score += result.scoreDelta;
        }
        return changed;
    }

    private MoveResult mergeLine(int[] line) {
        int[] merged = new int[SIZE];
        boolean[] alreadyMerged = new boolean[SIZE];
        int writeIndex = 0;
        int scoreDelta = 0;

        for (int readIndex = 0; readIndex < SIZE; readIndex++) {
            int value = line[readIndex];
            if (value == 0) {
                continue;
            }
            if (writeIndex > 0 && merged[writeIndex - 1] == value && !alreadyMerged[writeIndex - 1]) {
                merged[writeIndex - 1] *= 2;
                alreadyMerged[writeIndex - 1] = true;
                scoreDelta += merged[writeIndex - 1];
            } else {
                merged[writeIndex++] = value;
            }
        }

        return new MoveResult(merged, scoreDelta);
    }

    private boolean addRandomTile() {
        int emptyCount = 0;
        for (int value : cells) {
            if (value == 0) {
                emptyCount++;
            }
        }
        if (emptyCount == 0) {
            return false;
        }

        int selectedEmpty = ThreadLocalRandom.current().nextInt(emptyCount);
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] != 0) {
                continue;
            }
            if (selectedEmpty == 0) {
                cells[i] = ThreadLocalRandom.current().nextInt(10) == 0 ? 4 : 2;
                return true;
            }
            selectedEmpty--;
        }
        return false;
    }

    private boolean hasAvailableMove() {
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == 0) {
                return true;
            }
        }
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                int value = cells[index(row, col)];
                if (col + 1 < SIZE && cells[index(row, col + 1)] == value) {
                    return true;
                }
                if (row + 1 < SIZE && cells[index(row + 1, col)] == value) {
                    return true;
                }
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
        return new Game2048State(id, SIZE, score, gameOver, lastMoveChanged, movesMade, tiles);
    }

    private int index(int row, int col) {
        return row * SIZE + col;
    }

    private void touch() {
        lastTouched = Instant.now();
    }

    private record MoveResult(int[] values, int scoreDelta) {
    }
}
