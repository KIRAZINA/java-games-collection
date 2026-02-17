package com.KIRA_ZINA.app.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/**
 * Welcome screen with start game and exit buttons.
 * Features a modern green-black design with animated elements.
 */
public class WelcomeScreen extends JPanel {
    private JButton startButton;
    private JButton exitButton;
    private Runnable onStartGame;
    
    // Animation state
    private float titleGlow = 0.0f;
    private boolean glowIncreasing = true;
    private Timer glowTimer;
    
    /**
     * Creates a new welcome screen.
     * @param onStartGame Callback to run when start button is clicked
     */
    public WelcomeScreen(Runnable onStartGame) {
        this.onStartGame = onStartGame;
        
        setLayout(new GridBagLayout());
        setBackground(ColorScheme.BACKGROUND);
        
        initComponents();
        startGlowAnimation();
    }
    
    /**
     * Initializes UI components.
     */
    private void initComponents() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.insets = new Insets(20, 20, 20, 20);
        
        // Title panel
        JPanel titlePanel = createTitlePanel();
        gbc.gridy = 0;
        add(titlePanel, gbc);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Green-Black Edition");
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 20));
        subtitleLabel.setForeground(ColorScheme.NEON_GREEN);
        gbc.gridy = 1;
        add(subtitleLabel, gbc);
        
        // Buttons panel
        JPanel buttonsPanel = createButtonsPanel();
        gbc.gridy = 2;
        gbc.insets = new Insets(40, 20, 20, 20);
        add(buttonsPanel, gbc);
        
        // Instructions
        JLabel instructionsLabel = new JLabel("<html><center>Use Arrow Keys or WASD to play<br>Press F2 to restart</center></html>");
        instructionsLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        instructionsLabel.setForeground(new Color(100, 200, 100));
        instructionsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridy = 3;
        gbc.insets = new Insets(30, 20, 20, 20);
        add(instructionsLabel, gbc);
    }
    
    /**
     * Creates the animated title panel.
     */
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Draw glowing title
                String title = "2048";
                Font font = new Font("Arial", Font.BOLD, 120);
                g2d.setFont(font);
                
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(title);
                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() + fm.getAscent()) / 2 - fm.getDescent();
                
                // Glow effect
                int glowAlpha = (int) (titleGlow * 100);
                g2d.setColor(new Color(0, 255, 0, glowAlpha));
                for (int i = 8; i > 0; i--) {
                    g2d.drawString(title, x - i, y);
                    g2d.drawString(title, x + i, y);
                    g2d.drawString(title, x, y - i);
                    g2d.drawString(title, x, y + i);
                }
                
                // Main text
                g2d.setColor(ColorScheme.NEON_GREEN);
                g2d.drawString(title, x, y);
            }
        };
        
        panel.setPreferredSize(new Dimension(400, 150));
        panel.setBackground(ColorScheme.BACKGROUND);
        return panel;
    }
    
    /**
     * Creates the buttons panel.
     */
    private JPanel createButtonsPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(ColorScheme.BACKGROUND);
        
        // Start button
        startButton = createStyledButton("START GAME", e -> {
            if (onStartGame != null) {
                onStartGame.run();
            }
        });
        startButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(startButton);
        
        panel.add(Box.createVerticalStrut(20));
        
        // Exit button
        exitButton = createStyledButton("EXIT", e -> System.exit(0));
        exitButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(exitButton);
        
        return panel;
    }
    
    /**
     * Creates a styled button with green-black theme.
     */
    private JButton createStyledButton(String text, java.awt.event.ActionListener listener) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Background
                if (getModel().isPressed()) {
                    g2d.setColor(new Color(0, 200, 0));
                } else if (getModel().isRollover()) {
                    g2d.setColor(new Color(0, 255, 0));
                } else {
                    g2d.setColor(new Color(50, 100, 50));
                }
                
                g2d.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 15, 15));
                
                // Border
                g2d.setColor(ColorScheme.NEON_GREEN);
                g2d.setStroke(new BasicStroke(2));
                g2d.draw(new RoundRectangle2D.Float(1, 1, getWidth() - 2, getHeight() - 2, 15, 15));
                
                // Text - black when hovering or pressed, green otherwise
                if (getModel().isPressed() || getModel().isRollover()) {
                    g2d.setColor(Color.BLACK);
                } else {
                    g2d.setColor(ColorScheme.NEON_GREEN);
                }
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(getText());
                int textHeight = fm.getAscent();
                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() + textHeight) / 2 - fm.getDescent();
                g2d.drawString(getText(), x, y);
            }
        };
        
        button.setFont(new Font("Arial", Font.BOLD, 24));
        button.setPreferredSize(new Dimension(250, 60));
        button.setMaximumSize(new Dimension(250, 60));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(listener);
        
        return button;
    }
    
    /**
     * Starts the title glow animation.
     */
    private void startGlowAnimation() {
        glowTimer = new Timer(30, e -> {
            if (glowIncreasing) {
                titleGlow += 0.02f;
                if (titleGlow >= 1.0f) {
                    titleGlow = 1.0f;
                    glowIncreasing = false;
                }
            } else {
                titleGlow -= 0.02f;
                if (titleGlow <= 0.3f) {
                    titleGlow = 0.3f;
                    glowIncreasing = true;
                }
            }
            repaint();
        });
        glowTimer.start();
    }
    
    /**
     * Stops all animations.
     */
    public void stopAnimations() {
        if (glowTimer != null) {
            glowTimer.stop();
        }
    }
}
