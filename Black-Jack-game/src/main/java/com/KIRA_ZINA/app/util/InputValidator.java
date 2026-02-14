package com.KIRA_ZINA.app.util;

import com.KIRA_ZINA.app.exception.InvalidPlayerInputException;
import com.KIRA_ZINA.app.exception.InvalidBetException;
import com.KIRA_ZINA.app.model.Bankroll;

/**
 * Utility class for validating player input
 */
public class InputValidator {
    
    /**
     * Validates hit/stand input
     * @param input player input string
     * @return normalized input ("h" for hit, "s" for stand)
     * @throws InvalidPlayerInputException if input is invalid
     */
    public static String validateHitStandInput(String input) throws InvalidPlayerInputException {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidPlayerInputException("Input cannot be empty. Please enter 'h' for hit or 's' for stand.");
        }
        
        String normalized = input.trim().toLowerCase();
        
        if (normalized.equals("h") || normalized.equals("hit")) {
            return "h";
        } else if (normalized.equals("s") || normalized.equals("stand")) {
            return "s";
        } else {
            throw new InvalidPlayerInputException("Invalid input: '" + input + "'. Please enter 'h' for hit or 's' for stand.");
        }
    }
    
    /**
     * Validates yes/no input
     * @param input player input string
     * @return normalized input ("y" for yes, "n" for no)
     * @throws InvalidPlayerInputException if input is invalid
     */
    public static String validateYesNoInput(String input) throws InvalidPlayerInputException {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidPlayerInputException("Input cannot be empty. Please enter 'y' for yes or 'n' for no.");
        }
        
        String normalized = input.trim().toLowerCase();
        
        if (normalized.equals("y") || normalized.equals("yes")) {
            return "y";
        } else if (normalized.equals("n") || normalized.equals("no")) {
            return "n";
        } else {
            throw new InvalidPlayerInputException("Invalid input: '" + input + "'. Please enter 'y' for yes or 'n' for no.");
        }
    }
    
    /**
     * Validates bet amount
     * @param input player input string
     * @param bankroll player's bankroll for validation
     * @return validated bet amount
     * @throws InvalidPlayerInputException if input format is invalid
     * @throws InvalidBetException if bet amount is invalid
     */
    public static double validateBetAmount(String input, Bankroll bankroll) 
            throws InvalidPlayerInputException, InvalidBetException {
        
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidPlayerInputException("Bet amount cannot be empty.");
        }
        
        double amount;
        try {
            amount = Double.parseDouble(input.trim());
        } catch (NumberFormatException e) {
            throw new InvalidPlayerInputException("Invalid bet amount: '" + input + "'. Please enter a valid number.");
        }
        
        // Validate bet amount
        if (amount <= 0) {
            throw new InvalidBetException("Bet must be positive.");
        }
        
        if (amount < bankroll.getMinBet()) {
            throw new InvalidBetException("Minimum bet is $" + bankroll.getMinBet());
        }
        
        if (amount > bankroll.getMaxBet()) {
            throw new InvalidBetException("Maximum bet is $" + bankroll.getMaxBet());
        }
        
        if (amount > bankroll.getBalance()) {
            throw new InvalidBetException("Insufficient funds. Balance: $" + 
                                         String.format("%.2f", bankroll.getBalance()) + 
                                         ", Bet: $" + String.format("%.2f", amount));
        }
        
        return amount;
    }
    
    /**
     * Validates that a string is not null or empty
     * @param input string to validate
     * @param fieldName name of the field for error message
     * @throws InvalidPlayerInputException if input is invalid
     */
    public static void validateNotEmpty(String input, String fieldName) throws InvalidPlayerInputException {
        if (input == null || input.trim().isEmpty()) {
            throw new InvalidPlayerInputException(fieldName + " cannot be empty.");
        }
    }
}
