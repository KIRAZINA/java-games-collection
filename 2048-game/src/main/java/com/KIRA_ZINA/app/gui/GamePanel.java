package com.KIRA_ZINA.app.gui;

import com.KIRA_ZINA.app.model.Board;
import com.KIRA_ZINA.app.model.Direction;
import com.KIRA_ZINA.app.model.Tile;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;

/**
 * Main game panel that handles rendering, animations, and user input.
 * Uses double buffering for smooth animations.
 */
public class GamePanel extends JPanel {
    private static final int TILE_MARGIN = 15;
    private static final int TILE_ARC = 15; // Rounded corner radius
    private static final int ANIMATION_DURATION = 200; // milliseconds
    private static final int FPS = 60;
    
    private Board board;
    private int tileSize;
    private int gridSize;
    
    // Animation state
    private boolean animating;
    private long animationStartTime;
    private Timer animationTimer;
    
    /**
     * Creates a new game panel.
     */
    public GamePanel(Board board) {
        this.board = board;
        this.gridSize = Board.GRID_SIZE;
        
        setBackground(ColorScheme.BACKGROUND);
        setFocusable(true);
        setPreferredSize(new Dimension(600, 600));
        
        // Set up key listener for controls
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (!animating && !board.isGameOver()) {
                    handleKeyPress(e.getKeyCode());
                }
            }
        });
        
        // Set up animation timer (60 FPS)
        animationTimer = new Timer(1000 / FPS, e -> {
            if (animating) {
                repaint();
                
                // Check if animation is complete
                long elapsed = System.currentTimeMillis() - animationStartTime;
                if (elapsed >= ANIMATION_DURATION) {
                    animating = false;
                    animationTimer.stop();
                    
                    // Reset new tile flags after animation
                    for (Tile tile : board.getTiles()) {
                        tile.setNew(false);
                        tile.setScale(1.0);
                    }
                    repaint();
                }
            }
        });
    }
    
    /**
     * Handles keyboard input for game controls.
     * Supports both Arrow keys and WASD.
     */
    private void handleKeyPress(int keyCode) {
        Direction direction = null;
        
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                direction = Direction.UP;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                direction = Direction.DOWN;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                direction = Direction.LEFT;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                direction = Direction.RIGHT;
                break;
        }
        
        if (direction != null) {
            boolean moved = board.move(direction);
            if (moved) {
                startAnimation();
            }
        }
    }
    
    /**
     * Starts the tile movement animation.
     */
    private void startAnimation() {
        animating = true;
        animationStartTime = System.currentTimeMillis();
        
        // Initialize tile positions for animation
        updateTilePositions();
        
        animationTimer.start();
    }
    
    /**
     * Updates the animated positions of all tiles based on their grid positions.
     */
    private void updateTilePositions() {
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int gridPixelSize = Math.min(panelWidth, panelHeight) - TILE_MARGIN * 2;
        tileSize = (gridPixelSize - TILE_MARGIN * (gridSize + 1)) / gridSize;
        
        int offsetX = (panelWidth - gridPixelSize) / 2;
        int offsetY = (panelHeight - gridPixelSize) / 2;
        
        for (Tile tile : board.getTiles()) {
            double targetX = offsetX + TILE_MARGIN + tile.getCol() * (tileSize + TILE_MARGIN);
            double targetY = offsetY + TILE_MARGIN + tile.getRow() * (tileSize + TILE_MARGIN);
            
            // If tile doesn't have an animated position yet, set it to target (no animation)
            if (tile.getAnimatedX() == 0 && tile.getAnimatedY() == 0) {
                tile.setAnimatedPosition(targetX, targetY);
            } else {
                tile.setTargetPosition(targetX, targetY);
            }
        }
    }
    
    /**
     * Paints the game board and tiles.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Enable anti-aliasing for smooth graphics
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int gridPixelSize = Math.min(panelWidth, panelHeight) - TILE_MARGIN * 2;
        tileSize = (gridPixelSize - TILE_MARGIN * (gridSize + 1)) / gridSize;
        
        int offsetX = (panelWidth - gridPixelSize) / 2;
        int offsetY = (panelHeight - gridPixelSize) / 2;
        
        // Draw grid background
        g2d.setColor(ColorScheme.GRID_BACKGROUND);
        g2d.fillRoundRect(offsetX, offsetY, gridPixelSize, gridPixelSize, TILE_ARC, TILE_ARC);
        
        // Draw empty cells
        g2d.setColor(ColorScheme.EMPTY_CELL);
        for (int row = 0; row < gridSize; row++) {
            for (int col = 0; col < gridSize; col++) {
                int x = offsetX + TILE_MARGIN + col * (tileSize + TILE_MARGIN);
                int y = offsetY + TILE_MARGIN + row * (tileSize + TILE_MARGIN);
                g2d.fillRoundRect(x, y, tileSize, tileSize, TILE_ARC, TILE_ARC);
            }
        }
        
        // Draw tiles with animation
        List<Tile> tiles = board.getTiles();
        
        // Update positions if not animating
        if (!animating) {
            updateTilePositions();
        }
        
        for (Tile tile : tiles) {
            drawTile(g2d, tile);
        }
        
        // Draw game over overlay
        if (board.isGameOver()) {
            drawGameOver(g2d, panelWidth, panelHeight);
        }
    }
    
    /**
     * Draws a single tile with animation support including rotation and fade effects.
     */
    private void drawTile(Graphics2D g2d, Tile tile) {
        double x = tile.getAnimatedX();
        double y = tile.getAnimatedY();
        
        // Update animation progress
        if (animating) {
            long elapsed = System.currentTimeMillis() - animationStartTime;
            double progress = Math.min(1.0, (double) elapsed / ANIMATION_DURATION);
            
            // Smooth easing function (ease-out)
            progress = 1 - Math.pow(1 - progress, 3);
            
            tile.updateAnimation(progress);
            x = tile.getAnimatedX();
            y = tile.getAnimatedY();
            
            // Pop and rotation animation for new tiles
            if (tile.isNew()) {
                double scale;
                double rotation;
                if (progress < 0.5) {
                    scale = 0.8 + (progress * 0.6); // 0.8 to 1.1
                    rotation = progress * 720; // 0 to 360 degrees
                } else {
                    scale = 1.1 - ((progress - 0.5) * 0.2); // 1.1 to 1.0
                    rotation = 360 + (progress - 0.5) * 720; // 360 to 720 degrees
                }
                tile.setScale(scale);
                tile.setRotation(rotation);
                
                // Fade in effect
                tile.setOpacity((float) progress);
            }
        }
        
        double scale = tile.getScale();
        int scaledSize = (int) (tileSize * scale);
        int scaleOffset = (tileSize - scaledSize) / 2;
        
        // Save original transform
        java.awt.geom.AffineTransform oldTransform = g2d.getTransform();
        
        // Apply rotation if tile is new
        if (tile.isNew() && animating) {
            int centerX = (int) (x + tileSize / 2);
            int centerY = (int) (y + tileSize / 2);
            g2d.rotate(Math.toRadians(tile.getRotation()), centerX, centerY);
        }
        
        // Apply opacity
        Composite oldComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, tile.getOpacity()));
        
        // Draw tile background
        g2d.setColor(ColorScheme.getTileColor(tile.getValue()));
        g2d.fillRoundRect(
            (int) x + scaleOffset,
            (int) y + scaleOffset,
            scaledSize,
            scaledSize,
            TILE_ARC,
            TILE_ARC
        );
        
        // Draw tile value
        g2d.setColor(ColorScheme.getTextColor(tile.getValue()));
        String value = String.valueOf(tile.getValue());
        
        // Scale font based on tile size and value length
        int fontSize = tileSize / 2;
        if (value.length() > 2) {
            fontSize = tileSize / 3;
        }
        if (value.length() > 3) {
            fontSize = tileSize / 4;
        }
        
        Font font = new Font("Arial", Font.BOLD, (int) (fontSize * scale));
        g2d.setFont(font);
        
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(value);
        int textHeight = fm.getAscent();
        
        int textX = (int) (x + (tileSize - textWidth) / 2);
        int textY = (int) (y + (tileSize + textHeight) / 2 - fm.getDescent());
        
        g2d.drawString(value, textX, textY);
        
        // Restore original composite and transform
        g2d.setComposite(oldComposite);
        g2d.setTransform(oldTransform);
    }
    
    /**
     * Draws the game over overlay with green-black theme.
     */
    private void drawGameOver(Graphics2D g2d, int width, int height) {
        // Semi-transparent dark overlay
        g2d.setColor(new Color(0, 0, 0, 220));
        g2d.fillRect(0, 0, width, height);
        
        // Game over text with glow effect
        g2d.setColor(ColorScheme.NEON_GREEN);
        Font font = new Font("Arial", Font.BOLD, 60);
        g2d.setFont(font);
        
        String text = "Game Over!";
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textX = (width - textWidth) / 2;
        int textY = height / 2;
        
        // Glow effect
        g2d.setColor(new Color(0, 255, 0, 50));
        for (int i = 5; i > 0; i--) {
            g2d.drawString(text, textX - i, textY);
            g2d.drawString(text, textX + i, textY);
        }
        
        // Main text
        g2d.setColor(ColorScheme.NEON_GREEN);
        g2d.drawString(text, textX, textY);
        
        // Instruction text
        font = new Font("Arial", Font.PLAIN, 24);
        g2d.setFont(font);
        text = "Press F2 or click 'New Game' to restart";
        fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(text);
        textX = (width - textWidth) / 2;
        textY = height / 2 + 60;
        
        g2d.setColor(new Color(100, 200, 100));
        g2d.drawString(text, textX, textY);
    }
    
    /**
     * Starts a new game.
     */
    public void newGame() {
        board.reset();
        animating = false;
        if (animationTimer.isRunning()) {
            animationTimer.stop();
        }
        repaint();
        requestFocusInWindow();
    }
}
