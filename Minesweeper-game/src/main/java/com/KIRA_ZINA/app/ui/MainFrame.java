package com.KIRA_ZINA.app.ui;

import com.KIRA_ZINA.app.model.MinesweeperModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.KeyStroke;
import java.awt.event.KeyEvent;

/**
 * Main application window with Menu Bar and game container.
 */
public class MainFrame extends JFrame {
    // High contrast color scheme
    private static final Color WINDOW_BACKGROUND = new Color(230, 230, 230); // Darker background
    private static final Color MENU_BACKGROUND = new Color(245, 245, 245); // Lighter menu
    private static final Color MENU_BORDER = new Color(128, 128, 128); // Stronger border
    
    private GamePanel currentGamePanel;
    private TopPanel topPanel;
    private JPanel gameContainer;
    private int currentDifficultyIndex = 0; // Track current difficulty

    // Difficulty Presets
    private static final int[][] DIFFICULTIES = {
            {9, 9, 10},    // Beginner
            {16, 16, 40},  // Intermediate
            {16, 30, 99}   // Expert
    };

    public MainFrame() {
        this(0); // Default to Beginner
    }
    
    public MainFrame(int difficultyIndex) {
        super("Minesweeper - Modern Edition");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        
        // Modern window styling
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // Fallback to default
        }
        
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                showModernExitDialog();
            }
        });

        // Top Panel (Timer, Smiley, Counter)
        topPanel = new TopPanel();
        
        // Create Menu Bar
        JMenuBar menuBar = createHighContrastMenuBar();
        setJMenuBar(menuBar);

        // Game Container with high contrast styling
        gameContainer = new JPanel();
        gameContainer.setLayout(new BorderLayout());
        gameContainer.setBackground(WINDOW_BACKGROUND);
        gameContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(MENU_BORDER, 3),
            BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));
        gameContainer.add(topPanel, BorderLayout.NORTH);

        // Start with the selected difficulty
        startNewGame(difficultyIndex);

        add(gameContainer);
        pack();
        setLocationRelativeTo(null); // Center on screen
        setResizable(false);
        setVisible(true);
    }

    private void showModernExitDialog() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to exit the game?",
                "Exit Minesweeper",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
    
    private void showModernAboutDialog() {
        JOptionPane.showMessageDialog(
                this,
                "<html><div style='text-align: center;'>"
                + "<h2 style='color: #4285F4; margin: 10px 0;'>Minesweeper Modern Edition</h2>"
                + "<p style='color: #5F6368; margin: 5px 0;'>Built with Java 17 + Modern Swing</p>"
                + "<p style='color: #5F6368; margin: 5px 0;'>Enhanced UI with contemporary design</p>"
                + "<p style='color: #5F6368; margin: 5px 0; font-size: 12px;'>Version 2.0 | 2024</p>"
                + "</div></html>",
                "About Minesweeper",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private JMenuBar createHighContrastMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBackground(MENU_BACKGROUND);
        menuBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 3, 0, MENU_BORDER),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Game Menu with high contrast
        JMenu gameMenu = new JMenu("Game");
        gameMenu.setMnemonic(KeyEvent.VK_G);
        gameMenu.setFont(new Font("Segoe UI", Font.BOLD, 15));
        gameMenu.setForeground(new Color(33, 37, 41));
        gameMenu.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JMenuItem newGameItem = createHighContrastMenuItem("New Game", KeyEvent.VK_F2);
        newGameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        newGameItem.addActionListener(e -> startNewGame(getCurrentDifficultyIndex()));
        gameMenu.add(newGameItem);

        gameMenu.addSeparator();

        // Difficulty Submenu with high contrast
        JMenu difficultyMenu = new JMenu("Difficulty");
        difficultyMenu.setFont(new Font("Segoe UI", Font.BOLD, 15));
        difficultyMenu.setForeground(new Color(33, 37, 41));
        String[] difficultyNames = {"Beginner", "Intermediate", "Expert"};

        ButtonGroup difficultyGroup = new ButtonGroup();
        for (int i = 0; i < difficultyNames.length; i++) {
            JRadioButtonMenuItem item = createHighContrastRadioButtonMenuItem(difficultyNames[i]);
            if (i == 0) item.setSelected(true);
            final int index = i;
            item.addActionListener(e -> startNewGame(index));
            difficultyMenu.add(item);
            difficultyGroup.add(item);
        }
        gameMenu.add(difficultyMenu);

        gameMenu.addSeparator();

        JMenuItem exitItem = createHighContrastMenuItem("Exit", KeyEvent.VK_F4);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));
        exitItem.addActionListener(e -> showModernExitDialog());
        gameMenu.add(exitItem);

        menuBar.add(gameMenu);

        // Help Menu with high contrast
        JMenu helpMenu = new JMenu("Help");
        helpMenu.setFont(new Font("Segoe UI", Font.BOLD, 15));
        helpMenu.setForeground(new Color(33, 37, 41));
        helpMenu.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        JMenuItem aboutItem = createHighContrastMenuItem("About", KeyEvent.VK_A);
        aboutItem.addActionListener(e -> showModernAboutDialog());
        helpMenu.add(aboutItem);
        menuBar.add(helpMenu);

        // Link reset button to new game
        topPanel.setOnResetListener(() -> startNewGame(getCurrentDifficultyIndex()));

        return menuBar;
    }

    private int getCurrentDifficultyIndex() {
        return currentDifficultyIndex;
    }

    private void startNewGame(int difficultyIndex) {
        // Validate difficulty index
        if (difficultyIndex < 0 || difficultyIndex >= DIFFICULTIES.length) {
            difficultyIndex = 0;
        }
        
        // Store current difficulty
        this.currentDifficultyIndex = difficultyIndex;
        
        // Remove old game panel
        if (currentGamePanel != null) {
            gameContainer.remove(currentGamePanel);
        }

        // Get difficulty settings
        int[] settings = DIFFICULTIES[difficultyIndex];
        int rows = settings[0];
        int cols = settings[1];
        int mines = settings[2];

        // Reset top panel
        topPanel.resetTimer();

        // Create new game panel
        currentGamePanel = new GamePanel(rows, cols, mines, topPanel);
        gameContainer.add(currentGamePanel, BorderLayout.CENTER);

        // Adjust window size to fit grid
        pack();
        setLocationRelativeTo(null);

        // Store model reference for potential future use
        // currentModel = currentGamePanel.getModel(); // Would need getter
    }

    private JMenuItem createHighContrastMenuItem(String text, int mnemonic) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(new Font("Segoe UI", Font.BOLD, 14));
        item.setForeground(new Color(33, 37, 41));
        item.setMnemonic(mnemonic);
        item.setBackground(MENU_BACKGROUND);
        item.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        return item;
    }
    
    private JRadioButtonMenuItem createHighContrastRadioButtonMenuItem(String text) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(text);
        item.setFont(new Font("Segoe UI", Font.BOLD, 14));
        item.setForeground(new Color(33, 37, 41));
        item.setBackground(MENU_BACKGROUND);
        item.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        return item;
    }
}
