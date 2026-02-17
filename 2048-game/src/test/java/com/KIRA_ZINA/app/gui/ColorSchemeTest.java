package com.KIRA_ZINA.app.gui;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import static org.junit.jupiter.api.Assertions.*;

class ColorSchemeTest {

    @Test
    void testGetTileColor() {
        // Test specific mapped values
        assertEquals(new Color(30, 60, 30), ColorScheme.getTileColor(2));
        assertEquals(new Color(255, 215, 0), ColorScheme.getTileColor(2048));
        
        // Test default color for unknown value
        assertEquals(new Color(0, 255, 0), ColorScheme.getTileColor(99999));
    }

    @Test
    void testGetTextColor() {
        // Low values should have neon green text (for dark background)
        assertEquals(ColorScheme.NEON_GREEN, ColorScheme.getTextColor(2));
        assertEquals(ColorScheme.NEON_GREEN, ColorScheme.getTextColor(4));
        assertEquals(ColorScheme.NEON_GREEN, ColorScheme.getTextColor(256));
        
        // High values (>= 512) should have dark text (for bright background)
        assertEquals(ColorScheme.DARK_TEXT, ColorScheme.getTextColor(512));
        assertEquals(ColorScheme.DARK_TEXT, ColorScheme.getTextColor(1024));
        assertEquals(ColorScheme.DARK_TEXT, ColorScheme.getTextColor(2048));
    }
}
