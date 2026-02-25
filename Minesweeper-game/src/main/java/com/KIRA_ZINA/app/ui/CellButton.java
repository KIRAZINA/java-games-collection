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
    // High contrast color scheme
    private static final Color CELL_COVERED = new Color(255, 255, 255); // Pure white
    private static final Color CELL_COVERED_BORDER = new Color(128, 128, 128); // Stronger border
    private static final Color CELL_OPENED = new Color(235, 235, 235); // Darker opened
    private static final Color CELL_OPENED_BORDER = new Color(160, 160, 160); // Stronger opened border
    private static final Color CELL_HOVER = new Color(220, 220, 220); // Darker hover
    private static final Color CELL_PRESSED = new Color(200, 200, 200); // Much darker pressed
    private static final Color MINE_BACKGROUND = new Color(255, 220, 220); // Stronger red background
    private static final Color FLAG_COLOR = new Color(198, 40, 40); // Stronger red
    
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

        // High contrast button setup
        setFont(new Font("Segoe UI", Font.BOLD, 18));
        setFocusPainted(false);
        setMargin(new Insets(0, 0, 0, 0));
        setPreferredSize(new Dimension(30, 30));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CELL_COVERED_BORDER, 2),
            BorderFactory.createEmptyBorder(2, 2, 2, 2)
        ));
        setBackground(CELL_COVERED);
        setContentAreaFilled(false);
        setOpaque(false);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        // Initial visual state (Covered)
        updateVisuals();

        // Modern Mouse Handling with enhanced effects
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (gamePanel.isGameOver()) return;
                
                // LOGIC-005: Only show scared face for covered/flagged cells
                if (cell != null && (cell.isCovered() || cell.isFlagged())) {
                    gamePanel.onCellPressed();
                }
                
                // Visual feedback for pressed state with stronger contrast
                if (cell != null && cell.isCovered()) {
                    setBackground(CELL_PRESSED);
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CELL_COVERED_BORDER.darker(), 2),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ));
                }

                // Middle click (Chord) handling
                if (e.getButton() == MouseEvent.BUTTON2) {
                    gamePanel.chordAction(CellButton.this.row, CellButton.this.col);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (gamePanel != null && gamePanel.isGameOver()) return;
                // Tell TopPanel to show normal face
                if (gamePanel != null) gamePanel.onCellReleased();
                
                // Reset visual state with stronger borders
                if (cell != null && cell.isCovered()) {
                    setBackground(CELL_COVERED);
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CELL_COVERED_BORDER, 2),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ));
                }

                if (gamePanel != null) {
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        gamePanel.handleLeftClick(CellButton.this.row, CellButton.this.col);
                    } else if (e.getButton() == MouseEvent.BUTTON3) {
                        gamePanel.handleRightClick(CellButton.this.row, CellButton.this.col);
                    }
                }
            }
            
            @Override
            public void mouseEntered(MouseEvent e) {
                if (gamePanel != null && gamePanel.isGameOver()) return;
                // High contrast hover effect
                if (cell != null && cell.isCovered()) {
                    setBackground(CELL_HOVER);
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CELL_COVERED_BORDER.darker(), 2),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ));
                }
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                if (gamePanel.isGameOver()) return;
                // Reset hover effect with stronger borders
                if (cell != null && cell.isCovered()) {
                    setBackground(CELL_COVERED);
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(CELL_COVERED_BORDER, 2),
                        BorderFactory.createEmptyBorder(2, 2, 2, 2)
                    ));
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
        setEnabled(true);

        if (cell.isCovered()) {
            // Covered state: High contrast clean look
            setBackground(CELL_COVERED);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELL_COVERED_BORDER, 2),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ));
        } else if (cell.isFlagged()) {
            // Flagged state with high contrast styling
            setBackground(CELL_COVERED);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FLAG_COLOR, 2),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ));
            setText("🚩");
            setForeground(FLAG_COLOR);
        } else if (cell.isWrongFlag()) {
            // Wrong flag state (shown on game over) with high contrast
            setBackground(MINE_BACKGROUND);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(FLAG_COLOR, 2),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ));
            setText("❌");
            setForeground(FLAG_COLOR);
        } else if (cell.isOpened()) {
            // Opened state: High contrast flat look
            setBackground(CELL_OPENED);
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CELL_OPENED_BORDER, 2),
                BorderFactory.createEmptyBorder(2, 2, 2, 2)
            ));

            if (cell.isMine()) {
                setText("💣");
                setForeground(Color.BLACK);
                setBackground(MINE_BACKGROUND);
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(FLAG_COLOR, 3),
                    BorderFactory.createEmptyBorder(2, 2, 2, 2)
                ));
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
    
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        
        // Draw background
        g2d.setColor(getBackground());
        g2d.fillRect(0, 0, getWidth(), getHeight());
        
        // Draw strong border
        if (getBorder() != null) {
            g2d.setColor(getBackground() == CELL_COVERED ? CELL_COVERED_BORDER : CELL_OPENED_BORDER);
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRect(1, 1, getWidth() - 2, getHeight() - 2);
            
            // Add shadow effect for covered cells
            if (getBackground() == CELL_COVERED) {
                g2d.setColor(new Color(0, 0, 0, 50));
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRect(2, 2, getWidth() - 3, getHeight() - 3);
            }
        }
        
        // Draw text
        g2d.setColor(getForeground());
        g2d.setFont(getFont());
        FontMetrics fm = g2d.getFontMetrics();
        int textX = (getWidth() - fm.stringWidth(getText())) / 2;
        int textY = (getHeight() + fm.getAscent()) / 2;
        g2d.drawString(getText(), textX, textY);
    }
}
