package com.KIRA_ZINA.app.gui;

import com.KIRA_ZINA.app.model.Board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Main application window for the 2048 game with welcome screen.
 */
public class MainFrame extends JFrame {
    private Board board;
    private GamePanel gamePanel;
    private WelcomeScreen welcomeScreen;
    private JLabel scoreLabel;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    
    private static final String WELCOME_SCREEN = "welcome";
    private static final String GAME_SCREEN = "game";
    
    /**
     * Creates the main application window.
     */
    public MainFrame() {
        board = new Board();
        
        setTitle("2048 Game - Green-Black Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Set up card layout for screen switching
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        
        // Create welcome screen
        welcomeScreen = new WelcomeScreen(this::startGame);
        mainPanel.add(welcomeScreen, WELCOME_SCREEN);
        
        // Create game screen
        JPanel gameScreen = createGameScreen();
        mainPanel.add(gameScreen, GAME_SCREEN);
        
        add(mainPanel);
        
        // Show welcome screen first
        cardLayout.show(mainPanel, WELCOME_SCREEN);
        
        // Set up window
        pack();
        setLocationRelativeTo(null); // Center on screen
        setMinimumSize(new Dimension(600, 700));
        setResizable(true);
    }
    
    /**
     * Creates the game screen with top panel and game panel.
     */
    private JPanel createGameScreen() {
        JPanel screen = new JPanel(new BorderLayout());
        screen.setBackground(ColorScheme.BACKGROUND);
        
        // Create top panel with score and new game button
        JPanel topPanel = createTopPanel();
        screen.add(topPanel, BorderLayout.NORTH);
        
        // Create game panel
        gamePanel = new GamePanel(board);
        screen.add(gamePanel, BorderLayout.CENTER);
        
        // Add F2 key listener for new game
        screen.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_F2) {
                    newGame();
                }
            }
        });
        
        return screen;
    }
    
    /**
     * Creates the top panel with score display and buttons.
     */
    private JPanel createTopPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorScheme.BACKGROUND);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Title label
        JLabel titleLabel = new JLabel("2048");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 48));
        titleLabel.setForeground(ColorScheme.NEON_GREEN);
        
        // Score panel
        JPanel scorePanel = new JPanel();
        scorePanel.setLayout(new BoxLayout(scorePanel, BoxLayout.Y_AXIS));
        scorePanel.setBackground(new Color(50, 100, 50));
        scorePanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        JLabel scoreTitleLabel = new JLabel("SCORE");
        scoreTitleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        scoreTitleLabel.setForeground(ColorScheme.NEON_GREEN);
        scoreTitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        scoreLabel = new JLabel("0");
        scoreLabel.setFont(new Font("Arial", Font.BOLD, 24));
        scoreLabel.setForeground(ColorScheme.NEON_GREEN);
        scoreLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        scorePanel.add(scoreTitleLabel);
        scorePanel.add(scoreLabel);
        
        // Buttons panel
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonsPanel.setBackground(ColorScheme.BACKGROUND);
        
        // New game button
        JButton newGameButton = createStyledButton("New Game");
        newGameButton.addActionListener(e -> newGame());
        buttonsPanel.add(newGameButton);
        
        // Back to menu button
        JButton menuButton = createStyledButton("Menu");
        menuButton.addActionListener(e -> showWelcomeScreen());
        buttonsPanel.add(menuButton);
        
        // Right panel with score and buttons
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setBackground(ColorScheme.BACKGROUND);
        rightPanel.add(scorePanel);
        rightPanel.add(buttonsPanel);
        
        panel.add(titleLabel, BorderLayout.WEST);
        panel.add(rightPanel, BorderLayout.EAST);
        
        // Start score update timer
        Timer scoreTimer = new Timer(100, e -> updateScore());
        scoreTimer.start();
        
        return panel;
    }
    
    /**
     * Creates a styled button with green-black theme.
     */
    private JButton createStyledButton(String text) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(new Color(50, 100, 50));
        button.setForeground(ColorScheme.NEON_GREEN);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createLineBorder(ColorScheme.NEON_GREEN, 2));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Hover effect
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(70, 140, 70));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(50, 100, 50));
            }
        });
        
        return button;
    }
    
    /**
     * Starts the game from welcome screen.
     */
    private void startGame() {
        welcomeScreen.stopAnimations();
        cardLayout.show(mainPanel, GAME_SCREEN);
        gamePanel.requestFocusInWindow();
    }
    
    /**
     * Shows the welcome screen.
     */
    private void showWelcomeScreen() {
        cardLayout.show(mainPanel, WELCOME_SCREEN);
    }
    
    /**
     * Updates the score display.
     */
    private void updateScore() {
        if (scoreLabel != null) {
            scoreLabel.setText(String.valueOf(board.getScore()));
        }
    }
    
    /**
     * Starts a new game.
     */
    private void newGame() {
        gamePanel.newGame();
        updateScore();
    }
}
