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
    private MinesweeperModel currentModel;
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
        super("Minesweeper - Java Swing");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(
                        MainFrame.this,
                        "Are you sure you want to exit?",
                        "Exit",
                        JOptionPane.YES_NO_OPTION
                );
                if (result == JOptionPane.YES_OPTION) {
                    System.exit(0);
                }
            }
        });

        // Top Panel (Timer, Smiley, Counter)
        topPanel = new TopPanel();
        // Create Menu Bar
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        // Game Container (holds the grid)
        gameContainer = new JPanel();
        gameContainer.setLayout(new BorderLayout());
        gameContainer.add(topPanel, BorderLayout.NORTH);

        // Start with Beginner difficulty
        startNewGame(0);

        add(gameContainer);
        pack();
        setLocationRelativeTo(null); // Center on screen
        setResizable(false);
        setVisible(true);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Game Menu
        JMenu gameMenu = new JMenu("Game");
        gameMenu.setMnemonic(KeyEvent.VK_G);

        JMenuItem newGameItem = new JMenuItem("New Game");
        newGameItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0));
        newGameItem.addActionListener(e -> startNewGame(getCurrentDifficultyIndex()));
        gameMenu.add(newGameItem);

        gameMenu.addSeparator();

        // Difficulty Submenu
        JMenu difficultyMenu = new JMenu("Difficulty");
        String[] difficultyNames = {"Beginner", "Intermediate", "Expert"};

        ButtonGroup difficultyGroup = new ButtonGroup();
        for (int i = 0; i < difficultyNames.length; i++) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(difficultyNames[i]);
            if (i == 0) item.setSelected(true);
            final int index = i;
            item.addActionListener(e -> startNewGame(index));
            difficultyMenu.add(item);
            difficultyGroup.add(item);
        }
        gameMenu.add(difficultyMenu);

        gameMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));
        exitItem.addActionListener(e -> System.exit(0));
        gameMenu.add(exitItem);

        menuBar.add(gameMenu);

        // Help Menu
        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        aboutItem.addActionListener(e -> showAbout());
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

    private void showAbout() {
        JOptionPane.showMessageDialog(
                this,
                "Minesweeper Classic\nJava 17 + Swing\n\nCreated as a demo project.",
                "About",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
}
