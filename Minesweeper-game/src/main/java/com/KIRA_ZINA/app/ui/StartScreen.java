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
    private static final Color CLASSIC_BG = new Color(0xC0C0C0);
    private static final Color CLASSIC_DARK = new Color(0x808080);
    private static final Color TITLE_COLOR = new Color(0x000080);
    
    private final Runnable onStartGame;
    private final Runnable onExit;
    
    public StartScreen(Runnable onStartGame, Runnable onExit) {
        this.onStartGame = onStartGame;
        this.onExit = onExit;
        
        setLayout(new BorderLayout());
        setBackground(CLASSIC_BG);
        setBorder(BorderFactory.createRaisedBevelBorder());
        
        // Create main content
        add(createTitlePanel(), BorderLayout.NORTH);
        add(createCenterPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);
        
        // Add keyboard shortcuts
        registerKeyboardActions();
    }
    
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CLASSIC_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 20, 10, 20));
        
        // Mine icon
        JLabel mineIcon = new JLabel("💣", SwingConstants.CENTER);
        mineIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 64));
        panel.add(mineIcon, BorderLayout.NORTH);
        
        // Title
        JLabel titleLabel = new JLabel("MINESWEEPER", SwingConstants.CENTER);
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 42));
        titleLabel.setForeground(TITLE_COLOR);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 0, 5, 0));
        panel.add(titleLabel, BorderLayout.CENTER);
        
        // Subtitle
        JLabel subtitleLabel = new JLabel("Classic Edition", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("SansSerif", Font.ITALIC, 16));
        subtitleLabel.setForeground(CLASSIC_DARK);
        panel.add(subtitleLabel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createCenterPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CLASSIC_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 50, 20, 50));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Decorative mines row
        JPanel minesRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        minesRow.setBackground(CLASSIC_BG);
        String[] emojis = {"🚩", "💣", "🚩", "💣", "🚩"};
        for (String emoji : emojis) {
            JLabel label = new JLabel(emoji);
            label.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 28));
            minesRow.add(label);
        }
        panel.add(minesRow, gbc);
        
        gbc.gridy++;
        gbc.insets = new Insets(20, 5, 5, 5);
        
        // Instructions
        JLabel instructions = new JLabel("<html><div style='text-align: center;'>"
                + "Left click to open cells<br>"
                + "Right click to place flags<br>"
                + "Find all mines to win!"
                + "</div></html>", SwingConstants.CENTER);
        instructions.setFont(new Font("SansSerif", Font.PLAIN, 14));
        instructions.setForeground(Color.DARK_GRAY);
        panel.add(instructions, gbc);
        
        return panel;
    }
    
    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(CLASSIC_BG);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 50, 40, 50));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        
        // Start Game Button
        JButton startButton = createStyledButton("▶  Start Game", KeyEvent.VK_S);
        startButton.addActionListener(e -> {
            if (onStartGame != null) onStartGame.run();
        });
        panel.add(startButton, gbc);
        
        gbc.gridy++;
        
        // Exit Button
        JButton exitButton = createStyledButton("✕  Exit", KeyEvent.VK_X);
        exitButton.addActionListener(e -> {
            if (onExit != null) onExit.run();
        });
        panel.add(exitButton, gbc);
        
        // Version label
        gbc.gridy++;
        gbc.insets = new Insets(20, 0, 0, 0);
        JLabel versionLabel = new JLabel("v1.0 | Java Swing", SwingConstants.CENTER);
        versionLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
        versionLabel.setForeground(CLASSIC_DARK);
        panel.add(versionLabel, gbc);
        
        return panel;
    }
    
    private JButton createStyledButton(String text, int mnemonic) {
        JButton button = new JButton(text);
        button.setFont(new Font("SansSerif", Font.BOLD, 18));
        button.setPreferredSize(new Dimension(200, 50));
        button.setBackground(CLASSIC_BG);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createRaisedBevelBorder(),
                BorderFactory.createEmptyBorder(5, 15, 5, 15)
        ));
        button.setFocusPainted(false);
        button.setMnemonic(mnemonic);
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(0xD0D0D0));
            }
            
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(CLASSIC_BG);
            }
        });
        
        return button;
    }
    
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
