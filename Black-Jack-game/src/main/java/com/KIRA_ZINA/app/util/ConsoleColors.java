package com.KIRA_ZINA.app.util;

/**
 * Utility class for console colors and formatting
 */
public class ConsoleColors {
    
    // Color codes
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    
    // Background colors
    public static final String BLACK_BACKGROUND = "\u001B[40m";
    public static final String RED_BACKGROUND = "\u001B[41m";
    public static final String GREEN_BACKGROUND = "\u001B[42m";
    public static final String YELLOW_BACKGROUND = "\u001B[43m";
    public static final String BLUE_BACKGROUND = "\u001B[44m";
    public static final String PURPLE_BACKGROUND = "\u001B[45m";
    public static final String CYAN_BACKGROUND = "\u001B[46m";
    public static final String WHITE_BACKGROUND = "\u001B[47m";
    
    // Text formatting
    public static final String BOLD = "\u001B[1m";
    public static final String ITALIC = "\u001B[3m";
    public static final String UNDERLINE = "\u001B[4m";
    
    /**
     * Returns colored text
     * @param text the text to color
     * @param color the color code
     * @return colored text
     */
    public static String color(String text, String color) {
        return color + text + RESET;
    }
    
    /**
     * Returns bold text
     * @param text the text to make bold
     * @return bold text
     */
    public static String bold(String text) {
        return BOLD + text + RESET;
    }
    
    /**
     * Returns green text (for wins)
     * @param text the text to color green
     * @return green text
     */
    public static String green(String text) {
        return color(text, GREEN);
    }
    
    /**
     * Returns red text (for losses)
     * @param text the text to color red
     * @return red text
     */
    public static String red(String text) {
        return color(text, RED);
    }
    
    /**
     * Returns yellow text (for warnings/important info)
     * @param text the text to color yellow
     * @return yellow text
     */
    public static String yellow(String text) {
        return color(text, YELLOW);
    }
    
    /**
     * Returns cyan text (for info)
     * @param text the text to color cyan
     * @return cyan text
     */
    public static String cyan(String text) {
        return color(text, CYAN);
    }
    
    /**
     * Creates a separator line
     * @param character character to use for separator
     * @param length length of separator
     * @return separator string
     */
    public static String separator(char character, int length) {
        return String.valueOf(character).repeat(length);
    }
    
    /**
     * Creates a header with text
     * @param text header text
     * @return formatted header
     */
    public static String header(String text) {
        String separator = separator('=', 50);
        return "\n" + separator + "\n" + bold(text) + "\n" + separator + "\n";
    }
}
