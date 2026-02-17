package com.KIRA_ZINA.app;

import com.KIRA_ZINA.app.gui.MainFrame;

import javax.swing.*;

/**
 * Main entry point for the 2048 game application.
 * @author KIRA_ZINA
 */
public class Main {
    public static void main(String[] args) {
        // Run on Event Dispatch Thread for thread safety
        SwingUtilities.invokeLater(() -> {
            MainFrame frame = new MainFrame();
            frame.setVisible(true);
        });
    }
}