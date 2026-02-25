package com.KIRA_ZINA.app.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;

/**
 * Difficulty selection screen.
 * Allows user to choose game difficulty before starting.
 */
public class DifficultyScreen extends JPanel {
    // High contrast color scheme matching StartScreen
    private static final Color PRIMARY_COLOR = new Color(25, 118, 210); // Stronger Blue
    private static final Color ACCENT_COLOR = new Color(211, 47, 47); // Stronger Red
    private static final Color BACKGROUND_START = new Color(245, 245, 245); // Lighter gray
    private static final Color BACKGROUND_END = new Color(250, 250, 250); // Even lighter
    private static final Color TEXT_COLOR = new Color(13, 71, 161); // Very dark blue
    private static final Color SUBTITLE_COLOR = new Color(33, 37, 41); // Dark gray
    private static final Color BUTTON_HOVER = new Color(224, 224, 224); // Darker hover
    private static final Color BORDER_COLOR = new Color(189, 189, 189); // Stronger borders
    
    // Difficulty options with settings: {rows, cols, mines}
    private static final String[][] DIFFICULTIES = {
            {"Beginner", "9 x 9", "10 mines"},
            {"Intermediate", "16 x 16", "40 mines"},
            {"Expert", "16 x 30", "99 mines"}
    };
    
    private final Consumer<Integer> onDifficultySelected;
    private final Runnable onBack;
    
    public DifficultyScreen(Consumer<Integer> onDifficultySelected, Runnable onBack) {
        this.onDifficultySelected = onDifficultySelected;
        this.onBack = onBack;
        
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
            BorderFactory.createEmptyBorder(30, 20, 20, 20)
        ));
        
        // Back arrow with title
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(new Color(0, 0, 0, 0));
        
        // Title
        JLabel titleLabel = new JLabel("SELECT DIFFICULTY", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                super.paintComponent(g);
            }
        };
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createEmptyBorder(15, 0, 8, 0),
            BorderFactory.createMatteBorder(0, 0, 3, 0, PRIMARY_COLOR)
        ));
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        
        panel.add(titlePanel, BorderLayout.CENTER);
        
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
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Add difficulty buttons
        Color[] difficultyColors = {
            new Color(76, 175, 80),   // Green for Beginner
            new Color(255, 152, 0),  // Orange for Intermediate
            new Color(244, 67, 54)   // Red for Expert
        };
        
        for (int i = 0; i < DIFFICULTIES.length; i++) {
            JButton difficultyButton = createDifficultyButton(
                DIFFICULTIES[i][0],  // Name
                DIFFICULTIES[i][1],  // Size
                DIFFICULTIES[i][2],  // Mines
                difficultyColors[i],
                i
            );
            panel.add(difficultyButton, gbc);
            gbc.gridy++;
        }
        
        return panel;
    }
    
    private JButton createDifficultyButton(String name, String size, String mines, Color color, int index) {
        JButton button = new JButton();
        
        String buttonText = "<html><div style='text-align: center;'>"
            + "<span style='font-size: 20px; font-weight: bold; color: " + String.format("#%06X", 0xFFFFFF & color.getRGB()) + ";'>" + name + "</span><br>"
            + "<span style='font-size: 13px; color: #555555;'>" + size + " | " + mines + "</span>"
            + "</div></html>";
        
        button.setText(buttonText);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setPreferredSize(new Dimension(280, 75));
        button.setBackground(Color.WHITE);
        button.setForeground(color);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(color, 2),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        button.setFocusPainted(false);
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(240, 240, 240));
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color.darker(), 3),
                    BorderFactory.createEmptyBorder(9, 14, 9, 14)
                ));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(Color.WHITE);
                button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(color, 2),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
                ));
            }
            
            @Override
            public void mousePressed(java.awt.event.MouseEvent evt) {
                button.setBackground(color);
                button.setForeground(Color.WHITE);
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(240, 240, 240));
                button.setForeground(color);
            }
        });
        
        // Add action listener
        button.addActionListener(e -> {
            if (onDifficultySelected != null) {
                onDifficultySelected.accept(index);
            }
        });
        
        return button;
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
        
        // Back Button
        JButton backButton = createModernButton("← Back", KeyEvent.VK_B, SUBTITLE_COLOR);
        backButton.addActionListener(e -> {
            if (onBack != null) onBack.run();
        });
        panel.add(backButton, gbc);
        
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
        
        button.setFont(new Font("Segoe UI", Font.BOLD, 18));
        button.setPreferredSize(new Dimension(180, 55));
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
        
        // Hover effect
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
            }
            
            @Override
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                button.setBackground(BUTTON_HOVER);
                button.setForeground(color.darker());
            }
        });
        
        return button;
    }
    
    private transient java.awt.Shape shape;
    
    private void registerKeyboardActions() {
        // Escape to go back
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "back");
        getActionMap().put("back", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onBack != null) onBack.run();
            }
        });
        
        // Number keys 1-3 to select difficulty
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_1, 0), "select1");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_2, 0), "select2");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_3, 0), "select3");
        
        getActionMap().put("select1", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onDifficultySelected != null) onDifficultySelected.accept(0);
            }
        });
        getActionMap().put("select2", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onDifficultySelected != null) onDifficultySelected.accept(1);
            }
        });
        getActionMap().put("select3", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (onDifficultySelected != null) onDifficultySelected.accept(2);
            }
        });
    }
    
    // Functional interface for callback
    @FunctionalInterface
    public interface Consumer<T> {
        void accept(T t);
    }
}
