package com.KIRA_ZINA.app.gui;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * Defines the GREEN-BLACK color scheme for the 2048 game.
 * Uses green and black colors for a modern, high-contrast look.
 */
public class ColorScheme {
    // Background colors - green-black theme
    public static final Color BACKGROUND = new Color(15, 15, 15);  // Almost black
    public static final Color GRID_BACKGROUND = new Color(25, 25, 25);  // Dark gray
    public static final Color EMPTY_CELL = new Color(40, 40, 40);  // Darker gray
    
    // Text colors
    public static final Color DARK_TEXT = new Color(0, 0, 0);  // Pure black
    public static final Color LIGHT_TEXT = new Color(0, 255, 0);  // Bright green
    public static final Color NEON_GREEN = new Color(57, 255, 20);  // Neon green
    
    // Tile colors mapped by value - green-black gradient theme
    private static final Map<Integer, Color> TILE_COLORS = new HashMap<>();
    
    static {
        // Green-black gradient color scheme
        TILE_COLORS.put(2, new Color(30, 60, 30));         // Very dark green
        TILE_COLORS.put(4, new Color(40, 80, 40));         // Dark green
        TILE_COLORS.put(8, new Color(50, 100, 50));        // Medium-dark green
        TILE_COLORS.put(16, new Color(60, 120, 60));       // Medium green
        TILE_COLORS.put(32, new Color(70, 140, 70));       // Brighter green
        TILE_COLORS.put(64, new Color(80, 160, 80));       // Bright green
        TILE_COLORS.put(128, new Color(90, 180, 90));      // Very bright green
        TILE_COLORS.put(256, new Color(100, 200, 100));    // Light green
        TILE_COLORS.put(512, new Color(57, 255, 20));      // Neon green
        TILE_COLORS.put(1024, new Color(0, 255, 0));       // Pure green
        TILE_COLORS.put(2048, new Color(255, 215, 0));     // Gold (winner!)
        TILE_COLORS.put(4096, new Color(0, 255, 127));     // Spring green
        TILE_COLORS.put(8192, new Color(50, 205, 50));     // Lime green
    }
    
    /**
     * Gets the background color for a tile with the given value.
     */
    public static Color getTileColor(int value) {
        return TILE_COLORS.getOrDefault(value, new Color(0, 255, 0));
    }
    
    /**
     * Gets the text color for a tile with the given value.
     * All tiles use bright green or black text for contrast.
     */
    public static Color getTextColor(int value) {
        if (value >= 512) {
            return DARK_TEXT;  // Black text on bright tiles
        } else {
            return NEON_GREEN;  // Bright green text on dark tiles
        }
    }
}
