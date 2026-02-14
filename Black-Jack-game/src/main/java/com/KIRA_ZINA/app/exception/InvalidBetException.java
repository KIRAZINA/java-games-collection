package com.KIRA_ZINA.app.exception;

/**
 * Exception thrown when invalid bet is attempted
 */
public class InvalidBetException extends GameException {
    
    public InvalidBetException(String message) {
        super(message);
    }
    
    public InvalidBetException(String message, Throwable cause) {
        super(message, cause);
    }
}
