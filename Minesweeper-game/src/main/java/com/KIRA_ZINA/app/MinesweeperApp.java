package com.KIRA_ZINA.app;

import com.KIRA_ZINA.app.ui.MainFrame;
import com.KIRA_ZINA.app.ui.StartScreen;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main entry point for the Minesweeper application.
 * Manages the start screen and game window transitions.
 */
public class MinesweeperApp {
    private static JFrame mainFrame;
    private static boolean gameStarted = false;
    
    public static void main(String[] args) {
        // Set system look and feel for native appearance
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Run on Event Dispatch Thread (EDT) for thread safety
        SwingUtilities.invokeLater(() -> {
            showStartScreen();
        });
    }
    
    private static void showStartScreen() {
        mainFrame = new JFrame("Minesweeper");
        mainFrame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmExit();
            }
        });
        
        StartScreen startScreen = new StartScreen(
            () -> startGame(),  // onStartGame
            () -> confirmExit() // onExit
        );
        
        mainFrame.setContentPane(startScreen);
        mainFrame.pack();
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setResizable(false);
        mainFrame.setVisible(true);
    }
    
    private static void startGame() {
        if (gameStarted) return;
        gameStarted = true;
        
        // Hide start screen frame
        mainFrame.setVisible(false);
        mainFrame.dispose();
        
        // Create and show game window
        SwingUtilities.invokeLater(() -> {
            MainFrame gameFrame = new MainFrame();
            // Add window listener for returning to start screen
            gameFrame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    confirmExit();
                }
            });
        });
    }
    
    private static void confirmExit() {
        int result = JOptionPane.showConfirmDialog(
            mainFrame,
            "Are you sure you want to exit?",
            "Exit Game",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        );
        
        if (result == JOptionPane.YES_OPTION) {
            System.exit(0);
        }
    }
}
