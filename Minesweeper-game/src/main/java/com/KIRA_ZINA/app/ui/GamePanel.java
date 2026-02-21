package com.KIRA_ZINA.app.ui;

import com.KIRA_ZINA.app.model.Cell;
import com.KIRA_ZINA.app.model.MinesweeperModel;

import javax.swing.*;
import java.awt.*;

/**
 * Main game grid panel. Connects Model and View components.
 */
public class GamePanel extends JPanel implements MinesweeperModel.GameListener {
    private final MinesweeperModel model;
    private final TopPanel topPanel;
    private final CellButton[][] buttons;

    private boolean gameOver;

    public GamePanel(int rows, int cols, int mines, TopPanel topPanel) {
        this.topPanel = topPanel;
        this.gameOver = false;

        // Initialize Model
        this.model = new MinesweeperModel(rows, cols, mines);
        this.model.addListener(this);

        // Initialize UI Grid
        this.buttons = new CellButton[rows][cols];
        setLayout(new GridLayout(rows, cols));
        setBackground(Color.DARK_GRAY);
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 4));

        // Create Buttons
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                CellButton btn = new CellButton(r, c, this);
                buttons[r][c] = btn;
                add(btn);
            }
        }

        // Sync initial state
        updateMineCounter();
    }

    public boolean isGameOver() {
        return gameOver;
    }

    /**
     * Called by CellButton when mouse is pressed on any cell.
     */
    public void onCellPressed() {
        topPanel.setScaredFace();
    }

    /**
     * Called by CellButton when mouse is released on any cell.
     */
    public void onCellReleased() {
        topPanel.setNormalFace();
    }

    /**
     * Handles Left Click logic via Model.
     */
    public void handleLeftClick(int r, int c) {
        if (gameOver) return;

        // Start timer on first valid click
        if (!model.isFirstClickDone()) {
            topPanel.startTimer();
        }

        model.openCell(r, c);
    }

    /**
     * Handles Right Click logic via Model.
     */
    public void handleRightClick(int r, int c) {
        if (gameOver) return;
        model.toggleFlag(r, c);
        updateMineCounter();
    }

    /**
     * Handles Middle Click (Chord) logic.
     * Opens neighbors if flags count matches cell number.
     */
    public void chordAction(int r, int c) {
        if (gameOver) return;
        Cell cell = model.getCell(r, c);
        if (!cell.isOpened() || cell.getAdjacentMinesCount() == 0) return;

        int flaggedNeighbors = 0;
        // Count flagged neighbors
        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {
                if (dr == 0 && dc == 0) continue;
                int nr = r + dr, nc = c + dc;
                if (model.isValid(nr, nc) && model.getCell(nr, nc).isFlagged()) {
                    flaggedNeighbors++;
                }
            }
        }

        // If flags match number, open neighbors
        if (flaggedNeighbors == cell.getAdjacentMinesCount()) {
            for (int dr = -1; dr <= 1; dr++) {
                for (int dc = -1; dc <= 1; dc++) {
                    if (dr == 0 && dc == 0) continue;
                    int nr = r + dr, nc = c + dc;
                    if (model.isValid(nr, nc)) {
                        // Reuse logic from model but bypass first-click safety check as game started
                        // We call openCell directly, model handles safety internally
                        model.openCell(nr, nc);
                    }
                }
            }
        }
    }

    private void updateMineCounter() {
        topPanel.updateMineCounter(model.getRemainingMines());
    }

    public void resetGame() {
        model.reset();
        gameOver = false;
        topPanel.resetTimer();
        // Force visual update of all cells
        onFullRefresh();
    }

    // --- GameListener Implementation ---

    @Override
    public void onGameStateChanged(boolean won, boolean lost) {
        gameOver = true;
        if (won) {
            topPanel.setCoolFace();
        } else {
            topPanel.setSadFace();
        }
    }

    @Override
    public void onCellUpdated(int r, int c) {
        buttons[r][c].setModelCell(model.getCell(r, c));
        buttons[r][c].updateVisuals();
    }

    @Override
    public void onFullRefresh() {
        for (int r = 0; r < model.getRows(); r++) {
            for (int c = 0; c < model.getCols(); c++) {
                buttons[r][c].setModelCell(model.getCell(r, c));
                buttons[r][c].updateVisuals();
            }
        }
        updateMineCounter();
    }

    // Expose model method for checking first click state
    public boolean isFirstClickDone() {
        return model.isFirstClickDone();
    }
}
