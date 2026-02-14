package com.KIRA_ZINA.app.exception;

/**
 * Exception thrown when invalid card operations are attempted
 */
public class InvalidCardOperationException extends GameException {
    
    public InvalidCardOperationException(String message) {
        super(message);
    }
    
    public InvalidCardOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
