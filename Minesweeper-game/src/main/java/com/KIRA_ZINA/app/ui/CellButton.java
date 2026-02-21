package com.KIRA_ZINA.app.ui;

import com.KIRA_ZINA.app.model.Cell;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Visual representation of a Cell using JButton.
 * Handles mouse interactions and visual state updates.
 */
public class CellButton extends JButton {
    private final int row;
    private final int col;
    private final GamePanel gamePanel;
    private Cell cell; // Reference to model cell

    // Colors for numbers 1-8 (Classic Minesweeper colors)
    private static final Color[] NUMBER_COLORS = {
            null,       // 0
            Color.BLUE, // 1
            Color.GREEN,// 2
            Color.RED,  // 3
            new Color(0, 0, 139), // 4 (Dark Blue)
            new Color(139, 0, 0), // 5 (Dark Red)
            new Color(0, 139, 139), // 6 (Cyan)
            Color.BLACK, // 7
            Color.GRAY   // 8
    };

    public CellButton(int row, int col, GamePanel gamePanel) {
        this.row = row;
        this.col = col;
        this.gamePanel = gamePanel;

        // Basic button setup
        setFont(new Font("SansSerif", Font.BOLD, 18));
        setFocusPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setPreferredSize(new Dimension(25, 25));

        // Initial visual state (Covered)
        updateVisuals();

        // Mouse Handling
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gamePanel.isGameOver()) return;
                
                // LOGIC-005: Only show scared face for covered/flagged cells
                if (cell != null && (cell.isCovered() || cell.isFlagged())) {
                    gamePanel.onCellPressed();
                }

                // Middle click (Chord) handling
                if (e.getButton() == MouseEvent.BUTTON2) {
                    gamePanel.chordAction(row, col);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (gamePanel.isGameOver()) return;
                // Tell TopPanel to show normal face
                gamePanel.onCellReleased();

                if (e.getButton() == MouseEvent.BUTTON1) {
                    gamePanel.handleLeftClick(row, col);
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    gamePanel.handleRightClick(row, col);
                }
            }
        });
    }

    public void setModelCell(Cell cell) {
        if (cell == null) return; // BUG-004: Null check
        this.cell = cell;
        updateVisuals();
    }

    /**
     * Updates the button text, icon, and background based on Cell state.
     */
    public void updateVisuals() {
        if (cell == null) return;

        setText("");
        setIcon(null);
        setBackground(Color.LIGHT_GRAY);
        setEnabled(true);
        setBorder(UIManager.getBorder("Button.border")); // Default bevel

        if (cell.isCovered()) {
            // Covered state: Raised look (default button border)
            setBorder(UIManager.getBorder("Button.border"));
        } else if (cell.isFlagged()) {
            // Flagged state
            setText("🚩");
            setForeground(Color.RED);
        } else if (cell.isWrongFlag()) {
            // Wrong flag state (shown on game over)
            setText("❌");
            setForeground(Color.RED);
            setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            setBackground(new Color(0xC0C0C0));
        } else if (cell.isOpened()) {
            // Opened state: Flat look
            setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
            setBackground(new Color(0xC0C0C0)); // Classic gray

            if (cell.isMine()) {
                setText("💣");
                setForeground(Color.BLACK);
                setBackground(Color.RED); // Exploded mine
            } else {
                int count = cell.getAdjacentMinesCount();
                if (count > 0) {
                    setText(String.valueOf(count));
                    setForeground(NUMBER_COLORS[count]);
                } else {
                    setText(""); // Empty cell
                }
            }
        }
    }
}
