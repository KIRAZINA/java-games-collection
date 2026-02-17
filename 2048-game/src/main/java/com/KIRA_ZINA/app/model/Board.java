package com.KIRA_ZINA.app.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Represents the game board and contains all game logic.
 * Manages the grid, tile movements, merging, scoring, and game state.
 */
public class Board {
    // Grid size constant - change this to make the board larger/smaller
    public static final int GRID_SIZE = 4;
    
    private Tile[][] grid;
    private List<Tile> tiles; // All active tiles for rendering
    private int score;
    private boolean gameOver;
    private Random random;
    private boolean moved; // Flag to track if last move changed the board
    
    /**
     * Creates a new game board and initializes it with two random tiles.
     */
    public Board() {
        grid = new Tile[GRID_SIZE][GRID_SIZE];
        tiles = new ArrayList<>();
        score = 0;
        gameOver = false;
        random = new Random();
        
        // Start with two random tiles
        addRandomTile();
        addRandomTile();
    }
    
    /**
     * Resets the board for a new game.
     */
    public void reset() {
        grid = new Tile[GRID_SIZE][GRID_SIZE];
        tiles.clear();
        score = 0;
        gameOver = false;
        
        addRandomTile();
        addRandomTile();
    }
    
    /**
     * Adds a random tile (90% chance of 2, 10% chance of 4) to an empty cell.
     * @return true if a tile was added, false if the board is full
     */
    public boolean addRandomTile() {
        List<int[]> emptyCells = new ArrayList<>();
        
        // Find all empty cells
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (grid[row][col] == null) {
                    emptyCells.add(new int[]{row, col});
                }
            }
        }
        
        if (emptyCells.isEmpty()) {
            return false;
        }
        
        // Pick a random empty cell
        int[] cell = emptyCells.get(random.nextInt(emptyCells.size()));
        int value = random.nextDouble() < 0.9 ? 2 : 4;
        
        Tile tile = new Tile(value, cell[0], cell[1]);
        grid[cell[0]][cell[1]] = tile;
        tiles.add(tile);
        
        return true;
    }
    
    /**
     * Attempts to move all tiles in the specified direction.
     * @param direction The direction to move
     * @return true if the move changed the board state
     */
    public boolean move(Direction direction) {
        moved = false;
        boolean[][] merged = new boolean[GRID_SIZE][GRID_SIZE]; // Track merged cells
        
        switch (direction) {
            case UP:
                moveUp(merged);
                break;
            case DOWN:
                moveDown(merged);
                break;
            case LEFT:
                moveLeft(merged);
                break;
            case RIGHT:
                moveRight(merged);
                break;
        }
        
        if (moved) {
            addRandomTile();
            checkGameOver();
        }
        
        return moved;
    }
    
    /**
     * Moves all tiles up.
     */
    private void moveUp(boolean[][] merged) {
        for (int col = 0; col < GRID_SIZE; col++) {
            int targetRow = 0;
            
            for (int row = 0; row < GRID_SIZE; row++) {
                if (grid[row][col] != null) {
                    Tile tile = grid[row][col];
                    
                    // Check if we can merge with the tile above
                    if (targetRow > 0 && 
                        grid[targetRow - 1][col] != null && 
                        grid[targetRow - 1][col].getValue() == tile.getValue() &&
                        !merged[targetRow - 1][col]) {
                        
                        // Merge tiles
                        Tile target = grid[targetRow - 1][col];
                        target.setValue(target.getValue() * 2);
                        score += target.getValue();
                        merged[targetRow - 1][col] = true;
                        
                        grid[row][col] = null;
                        tile.setMerging(true);
                        tile.setRow(targetRow - 1);
                        moved = true;
                    } else {
                        // Just move the tile
                        if (row != targetRow) {
                            grid[row][col] = null;
                            grid[targetRow][col] = tile;
                            tile.setRow(targetRow);
                            moved = true;
                        }
                        targetRow++;
                    }
                }
            }
        }
        
        // Remove merged tiles
        tiles.removeIf(Tile::isMerging);
    }
    
    /**
     * Moves all tiles down.
     */
    private void moveDown(boolean[][] merged) {
        for (int col = 0; col < GRID_SIZE; col++) {
            int targetRow = GRID_SIZE - 1;
            
            for (int row = GRID_SIZE - 1; row >= 0; row--) {
                if (grid[row][col] != null) {
                    Tile tile = grid[row][col];
                    
                    // Check if we can merge with the tile below
                    if (targetRow < GRID_SIZE - 1 && 
                        grid[targetRow + 1][col] != null && 
                        grid[targetRow + 1][col].getValue() == tile.getValue() &&
                        !merged[targetRow + 1][col]) {
                        
                        // Merge tiles
                        Tile target = grid[targetRow + 1][col];
                        target.setValue(target.getValue() * 2);
                        score += target.getValue();
                        merged[targetRow + 1][col] = true;
                        
                        grid[row][col] = null;
                        tile.setMerging(true);
                        tile.setRow(targetRow + 1);
                        moved = true;
                    } else {
                        // Just move the tile
                        if (row != targetRow) {
                            grid[row][col] = null;
                            grid[targetRow][col] = tile;
                            tile.setRow(targetRow);
                            moved = true;
                        }
                        targetRow--;
                    }
                }
            }
        }
        
        // Remove merged tiles
        tiles.removeIf(Tile::isMerging);
    }
    
    /**
     * Moves all tiles left.
     */
    private void moveLeft(boolean[][] merged) {
        for (int row = 0; row < GRID_SIZE; row++) {
            int targetCol = 0;
            
            for (int col = 0; col < GRID_SIZE; col++) {
                if (grid[row][col] != null) {
                    Tile tile = grid[row][col];
                    
                    // Check if we can merge with the tile to the left
                    if (targetCol > 0 && 
                        grid[row][targetCol - 1] != null && 
                        grid[row][targetCol - 1].getValue() == tile.getValue() &&
                        !merged[row][targetCol - 1]) {
                        
                        // Merge tiles
                        Tile target = grid[row][targetCol - 1];
                        target.setValue(target.getValue() * 2);
                        score += target.getValue();
                        merged[row][targetCol - 1] = true;
                        
                        grid[row][col] = null;
                        tile.setMerging(true);
                        tile.setCol(targetCol - 1);
                        moved = true;
                    } else {
                        // Just move the tile
                        if (col != targetCol) {
                            grid[row][col] = null;
                            grid[row][targetCol] = tile;
                            tile.setCol(targetCol);
                            moved = true;
                        }
                        targetCol++;
                    }
                }
            }
        }
        
        // Remove merged tiles
        tiles.removeIf(Tile::isMerging);
    }
    
    /**
     * Moves all tiles right.
     */
    private void moveRight(boolean[][] merged) {
        for (int row = 0; row < GRID_SIZE; row++) {
            int targetCol = GRID_SIZE - 1;
            
            for (int col = GRID_SIZE - 1; col >= 0; col--) {
                if (grid[row][col] != null) {
                    Tile tile = grid[row][col];
                    
                    // Check if we can merge with the tile to the right
                    if (targetCol < GRID_SIZE - 1 && 
                        grid[row][targetCol + 1] != null && 
                        grid[row][targetCol + 1].getValue() == tile.getValue() &&
                        !merged[row][targetCol + 1]) {
                        
                        // Merge tiles
                        Tile target = grid[row][targetCol + 1];
                        target.setValue(target.getValue() * 2);
                        score += target.getValue();
                        merged[row][targetCol + 1] = true;
                        
                        grid[row][col] = null;
                        tile.setMerging(true);
                        tile.setCol(targetCol + 1);
                        moved = true;
                    } else {
                        // Just move the tile
                        if (col != targetCol) {
                            grid[row][col] = null;
                            grid[row][targetCol] = tile;
                            tile.setCol(targetCol);
                            moved = true;
                        }
                        targetCol--;
                    }
                }
            }
        }
        
        // Remove merged tiles
        tiles.removeIf(Tile::isMerging);
    }
    
    /**
     * Checks if the game is over (no valid moves remaining).
     */
    private void checkGameOver() {
        // Check for empty cells
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                if (grid[row][col] == null) {
                    return; // Game not over
                }
            }
        }
        
        // Check for possible merges
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                int value = grid[row][col].getValue();
                
                // Check right neighbor
                if (col < GRID_SIZE - 1 && grid[row][col + 1].getValue() == value) {
                    return; // Can merge
                }
                
                // Check bottom neighbor
                if (row < GRID_SIZE - 1 && grid[row + 1][col].getValue() == value) {
                    return; // Can merge
                }
            }
        }
        
        // No moves possible
        gameOver = true;
    }
    
    // Getters
    public Tile[][] getGrid() {
        return grid;
    }
    
    public List<Tile> getTiles() {
        return tiles;
    }
    
    public int getScore() {
        return score;
    }
    
    public boolean isGameOver() {
        return gameOver;
    }
}
