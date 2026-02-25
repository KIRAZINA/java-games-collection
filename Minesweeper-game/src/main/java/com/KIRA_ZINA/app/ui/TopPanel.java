package com.KIRA_ZINA.app.ui;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

/**
 * Top panel containing Timer, Mine Counter, and Reset Button (Smiley).
 */
public class TopPanel extends JPanel {
    // High contrast color scheme
    private static final Color PANEL_BACKGROUND = new Color(240, 240, 240); // Lighter background
    private static final Color LED_BACKGROUND = new Color(13, 27, 42); // Much darker background
    private static final Color LED_FOREGROUND = new Color(255, 87, 34); // Brighter orange-red
    private static final Color BORDER_COLOR = new Color(117, 117, 117); // Stronger border
    private static final Color SMILEY_NORMAL_COLOR = new Color(46, 125, 50); // Stronger green
    private static final Color SMILEY_HOVER_COLOR = new Color(27, 94, 32); // Darker green
    
    private final JLabel timerLabel;
    private final JLabel mineCounterLabel;
    private final JButton smileyButton;

    private int timeElapsed;
    private Timer timer;
    private boolean timerRunning;

    // Smiley States
    private static final String SMILEY_NORMAL = "🙂";
    private static final String SMILEY_SCARED = "😮";
    private static final String SMILEY_SAD    = "😵";
    private static final String SMILEY_COOL   = "😎";

    private Runnable onResetListener;

    public TopPanel() {
        setLayout(new BorderLayout());
        setBackground(PANEL_BACKGROUND);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 3),
            BorderFactory.createEmptyBorder(16, 20, 16, 20)
        ));

        timeElapsed = 0;
        timerRunning = false;

        // Mine Counter (Left)
        mineCounterLabel = createModernLedLabel("000");
        add(mineCounterLabel, BorderLayout.WEST);

        // Smiley Button (Center)
        smileyButton = createModernSmileyButton(SMILEY_NORMAL);
        add(smileyButton, BorderLayout.CENTER);

        // Timer (Right)
        timerLabel = createModernLedLabel("000");
        add(timerLabel, BorderLayout.EAST);

        // Game Timer
        timer = new Timer(1000, e -> {
            timeElapsed++;
            updateTimerDisplay();
        });
    }

    private JLabel createModernLedLabel(String text) {
        JLabel label = new JLabel(text, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int width = getWidth();
                int height = getHeight();
                
                // Draw rounded background with stronger contrast
                g2d.setColor(LED_BACKGROUND);
                g2d.fillRoundRect(0, 0, width, height, 10, 10);
                
                // Draw strong inner shadow effect
                g2d.setColor(new Color(0, 0, 0, 80));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(2, 2, width - 4, height - 4, 10, 10);
                
                // Draw outer border
                g2d.setColor(BORDER_COLOR);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(0, 0, width - 1, height - 1, 10, 10);
                
                // Draw text with enhanced LED effect
                g2d.setColor(LED_FOREGROUND);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (width - fm.stringWidth(getText())) / 2;
                int textY = (height + fm.getAscent()) / 2;
                g2d.drawString(getText(), textX, textY);
            }
        };
        label.setFont(new Font("Consolas", Font.BOLD, 32));
        label.setForeground(LED_FOREGROUND);
        label.setPreferredSize(new Dimension(90, 42));
        label.setOpaque(false);
        label.setBorder(BorderFactory.createEmptyBorder());
        return label;
    }

    public void setOnResetListener(Runnable listener) {
        this.onResetListener = listener;
    }

    private JButton createModernSmileyButton(String emoji) {
        JButton button = new JButton(emoji) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int width = getWidth();
                int height = getHeight();
                
                // Draw circular background with stronger contrast
                g2d.setColor(getBackground());
                g2d.fillOval(0, 0, width, height);
                
                // Draw strong border
                g2d.setColor(SMILEY_NORMAL_COLOR);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawOval(1, 1, width - 2, height - 2);
                
                // Add shadow effect
                g2d.setColor(new Color(0, 0, 0, 60));
                g2d.setStroke(new BasicStroke(1));
                g2d.drawOval(3, 3, width - 4, height - 4);
                
                // Draw emoji
                g2d.setColor(getForeground());
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int textX = (width - fm.stringWidth(getText())) / 2;
                int textY = (height + fm.getAscent()) / 2;
                g2d.drawString(getText(), textX, textY);
            }
            
            @Override
            public boolean contains(int x, int y) {
                if (shape == null || !shape.getBounds().equals(getBounds())) {
                    shape = new java.awt.geom.Ellipse2D.Float(0, 0, getWidth(), getHeight());
                }
                return shape.contains(x, y);
            }
        };
        
        button.setFont(new Font("Segoe UI Emoji", Font.BOLD, 36));
        button.setPreferredSize(new Dimension(56, 56));
        button.setBackground(Color.WHITE);
        button.setForeground(SMILEY_NORMAL_COLOR);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(SMILEY_NORMAL_COLOR, 3),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        button.addActionListener(e -> triggerReset());
        
        // Enhanced hover effect with stronger contrast
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(235, 235, 235));
                button.setForeground(SMILEY_HOVER_COLOR);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SMILEY_HOVER_COLOR, 3),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
                button.setForeground(SMILEY_NORMAL_COLOR);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SMILEY_NORMAL_COLOR, 3),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
            }
            
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBackground(SMILEY_NORMAL_COLOR);
                button.setForeground(Color.WHITE);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SMILEY_HOVER_COLOR, 3),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(235, 235, 235));
                button.setForeground(SMILEY_HOVER_COLOR);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(SMILEY_HOVER_COLOR, 3),
                    BorderFactory.createEmptyBorder(8, 8, 8, 8)
                ));
            }
        });
        
        return button;
    }
    
    private transient java.awt.Shape shape;

    private void triggerReset() {
        if (onResetListener != null) {
            onResetListener.run();
        }
    }

    public void startTimer() {
        if (!timerRunning) {
            timeElapsed = 0;
            timerRunning = true;
            updateTimerDisplay();
            timer.start();
        }
    }

    public void stopTimer() {
        timerRunning = false;
        timer.stop();
    }

    public void resetTimer() {
        stopTimer();
        timeElapsed = 0;
        updateTimerDisplay();
        setSmiley(SMILEY_NORMAL);
    }

    private void updateTimerDisplay() {
        // Cap at 999 seconds
        int displayTime = Math.min(timeElapsed, 999);
        timerLabel.setText(new DecimalFormat("000").format(displayTime));
    }

    public void updateMineCounter(int count) {
        // VISUAL-001: Fix negative number display
        int displayCount = Math.max(-99, Math.min(999, count));
        if (displayCount < 0) {
            mineCounterLabel.setText(String.format("%03d", displayCount));
        } else {
            mineCounterLabel.setText(new DecimalFormat("000").format(displayCount));
        }
    }

    public void setSmiley(String emoji) {
        smileyButton.setText(emoji);
    }

    public void setScaredFace() {
        // UX-005: Show scared face even before first click
        setSmiley(SMILEY_SCARED);
    }

    public void setNormalFace() {
        // UX-005: Show normal face even before first click
        setSmiley(SMILEY_NORMAL);
    }

    public void setSadFace() {
        stopTimer();
        setSmiley(SMILEY_SAD);
    }

    public void setCoolFace() {
        stopTimer();
        setSmiley(SMILEY_COOL);
    }
}
