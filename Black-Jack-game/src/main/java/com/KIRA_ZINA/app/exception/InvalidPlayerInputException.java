package com.KIRA_ZINA.app.exception;

/**
 * Exception thrown when player provides invalid input
 */
public class InvalidPlayerInputException extends GameException {
    
    public InvalidPlayerInputException(String message) {
        super(message);
    }
    
    public InvalidPlayerInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
