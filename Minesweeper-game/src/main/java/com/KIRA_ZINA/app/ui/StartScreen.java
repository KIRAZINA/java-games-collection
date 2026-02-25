package com.KIRA_ZINA.app.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Start screen with game title and main menu buttons.
 * Styled in classic Minesweeper theme.
 */
public class StartScreen extends JPanel {
    // High contrast color scheme
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210); // Stronger Blue
    private static final Color ACCENT_COLOR = new Color(211, 47, 47); // Stronger Red
    private static final Color BACKGROUND_START = new Color(245, 245, 245); // Lighter gray
    private static final Color BACKGROUND_END = new Color(250, 250, 250); // Even lighter
    private static final Color TEXT_COLOR = new Color(13, 71, 161); // Very dark blue
    private static final Color SUBTITLE_COLOR = new Color(33, 37, 41); // Dark gray
    private static final Color BUTTON_HOVER = new Color(224, 224, 224); // Darker hover
    private static final Color BORDER_COLOR = new Color(189, 189, 189); // Stronger borders
    
    private final Runnable onStartGame;
    private final Runnable onExit;
    
    public StartScreen(Runnable onStartGame, Runnable onExit) {
        this.onStartGame = onStartGame;
        this.onExit = onExit;
        
        setLayout(new BorderLayout());
        setBackground(BACKGROUND_START);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 2),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        
        // Create main content
        add(createTitlePanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        
        // Add keyboard shortcuts
        registerKeyboardActions();
    }
    
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int width = getWidth();
                int height = getHeight();
                GradientPaint gradient = new GradientPaint(0, 0, BACKGROUND_START, 0, height, BACKGROUND_END);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, width, height);
            }
        };
        panel.setBackground(BACKGROUND_START);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(40, 20, 20, 20)
        ));
        
        // Modern mine icon with shadow effect
        JLabel mineIcon = new JLabel("💣", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Add shadow
                g2d.setColor(new Color(0, 0, 0, 30));
                g2d.setFont(getFont().deriveFont(Font.BOLD, 66f));
                FontMetrics fm = g2d.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2 + 2;
                int y = (getHeight() + fm.getAscent()) / 2 + 2;
                g2d.drawString(getText(), x, y);
                // Draw text
                g2d.setColor(getForeground());
                g2d.drawString(getText(), x - 2, y - 2);
            }
        };
        mineIcon.setFont(new Font("Segoe UI Emoji", Font.BOLD, 64));
        mineIcon.setForeground(ACCENT_COLOR);
        panel.add(mineIcon, BorderLayout.NORTH);
        
        // Modern title with better typography
        JLabel titleLabel = new JLabel("MINESWEEPER", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                super.paintComponent(g);
            }
        };
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(15, 0, 8, 0),
            BorderFactory.createMatteBorder(0, 0, 3, 0, PRIMARY_COLOR)
        ));
        panel.add(titleLabel, BorderLayout.CENTER);
        
        // Modern subtitle
        JLabel subtitleLabel = new JLabel("Modern Edition", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        subtitleLabel.setForeground(SUBTITLE_COLOR);
        panel.add(subtitleLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int width = getWidth();
                int height = getHeight();
                GradientPaint gradient = new GradientPaint(0, 0, BACKGROUND_END, 0, height, BACKGROUND_START);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, width, height);
            }
        };
        panel.setBackground(BACKGROUND_START);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 2, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(30, 50, 30, 50)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Modern decorative mines with better spacing
        JPanel minesRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        minesRow.setBackground(new Color(0, 0, 0, 0)); // Transparent
        String[] emojis = {"🚩", "💣", "🚩", "💣", "🚩"};
        for (String emoji : emojis) {
            JLabel label = new JLabel(emoji);
            label.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
            minesRow.add(label);
        }
        panel.add(minesRow, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(25, 8, 8, 8);
        
        // Modern instructions with better typography and contrast
        JLabel instructions = new JLabel("<html><div style='text-align: center; color: #212121; font-family: Segoe UI;'>"
                + "<span style='font-size: 18px; font-weight: bold; color: #1976D2;'>How to Play</span><br><br>"
                + "<span style='font-size: 15px; font-weight: 500;'>"
                + "🖱️ Left click to reveal cells<br>"
                + "🚩 Right click to place flags<br>"
                + "🏆 Find all mines to win!"
                + "</span></div></html>", SwingConstants.CENTER);
        instructions.setFont(new Font("Segoe UI", Font.BOLD, 15));
        instructions.setForeground(SUBTITLE_COLOR);
        instructions.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));
        panel.add(instructions, gbc);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                int width = getWidth();
                int height = getHeight();
                GradientPaint gradient = new GradientPaint(0, 0, BACKGROUND_START, 0, height, BACKGROUND_END);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, width, height);
            }
        };
        panel.setBackground(BACKGROUND_START);
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, BORDER_COLOR),
            BorderFactory.createEmptyBorder(30, 50, 50, 50)
        ));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(12, 0, 12, 0);
        
        // Modern Start Game Button
        JButton startButton = createModernButton("▶ Start Game", KeyEvent.VK_S, PRIMARY_COLOR);
        startButton.addActionListener(e -> {
            if (onStartGame != null) onStartGame.run();
        });
        panel.add(startButton, gbc);
        
        gbc.gridy++;
        
        // Modern Exit Button
        JButton exitButton = createModernButton("✕ Exit", KeyEvent.VK_X, ACCENT_COLOR);
        exitButton.addActionListener(e -> {
            if (onExit != null) onExit.run();
        });
        panel.add(exitButton, gbc);
        
        // Modern version label with stronger contrast
        gbc.gridy++;
        gbc.insets = new Insets(25, 0, 0, 0);
        JLabel versionLabel = new JLabel("v2.0 | High Contrast Edition", SwingConstants.CENTER);
        versionLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        versionLabel.setForeground(SUBTITLE_COLOR);
        versionLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        panel.add(versionLabel, gbc);
        
        return panel;
    }
    
    private JButton createModernButton(String text, int mnemonic, Color color) {
        JButton button = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                
                int width = getWidth();
                int height = getHeight();
                
                // Draw rounded rectangle background
                g2d.setColor(getBackground());
                g2d.fillRoundRect(0, 0, width, height, 12, 12);
                
                // Draw strong border
                g2d.setColor(color);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(1, 1, width - 2, height - 2, 12, 12);
                
                // Add shadow effect
                g2d.setColor(new Color(0, 0, 0, 40));
                g2d.setStroke(new BasicStroke(1));
                g2d.drawRoundRect(3, 3, width - 4, height - 4, 12, 12);
                
                // Draw text
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
                    shape = new java.awt.geom.RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12);
                }
                return shape.contains(x, y);
            }
        };
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 20));
        button.setPreferredSize(new Dimension(240, 60));
        button.setBackground(Color.WHITE);
        button.setForeground(color);
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setOpaque(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 3),
            BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        button.setMnemonic(mnemonic);
        
        // Modern hover effect with stronger contrast
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
                button.setForeground(color.darker());
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color.darker(), 3),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
                button.setForeground(color);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color, 3),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
            
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
                button.setForeground(Color.WHITE);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color.darker(), 3),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
                button.setForeground(color.darker());
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color.darker(), 3),
                    BorderFactory.createEmptyBorder(10, 20, 10, 20)
                ));
            }
        });
        
        return button;
    }
    
    private transient java.awt.Shape shape;
    
    private void registerKeyboardActions() {
        // Enter to start game
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "startGame");
        getActionMap().put("startGame", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onStartGame != null) onStartGame.run();
            }
        });
        
        // Escape to exit
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "exit");
        getActionMap().put("exit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onExit != null) onExit.run();
            }
        });
    }
}
