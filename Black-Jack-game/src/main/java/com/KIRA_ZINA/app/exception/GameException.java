package com.KIRA_ZINA.app.exception;

/**
 * Base exception class for game-related errors
 */
public class GameException extends Exception {
    
    public GameException(String message) {
        super(message);
    }
    
    public GameException(String message, Throwable cause) {
        super(message, cause);
    }
}
