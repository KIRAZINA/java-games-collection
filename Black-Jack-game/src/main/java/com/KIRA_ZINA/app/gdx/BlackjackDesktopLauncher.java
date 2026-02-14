package com.KIRA_ZINA.app.gdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

/**
 * Desktop launcher for the Blackjack LibGDX application.
 * This is the entry point for running the game on desktop platforms.
 */
public class BlackjackDesktopLauncher {
    
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        
        // Window configuration
        config.setTitle("Blackjack - LibGDX Edition");
        config.setWindowedMode(1024, 768);
        config.setResizable(true);
        config.useVsync(true);
        config.setForegroundFPS(60);
        
        // Initialize the LibGDX application
        new Lwjgl3Application(new BlackjackGdxGame(), config);
    }
}