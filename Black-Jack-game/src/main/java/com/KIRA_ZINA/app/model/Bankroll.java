package com.KIRA_ZINA.app.model;

import com.KIRA_ZINA.app.exception.InvalidBetException;

/**
 * Represents player's bankroll and betting system
 */
public class Bankroll {
    private double balance;
    private double currentBet;
    private static final double MIN_BET = 1.0;
    private static final double MAX_BET = 1000.0;
    
    public Bankroll(double initialBalance) {
        if (initialBalance < MIN_BET) {
            throw new IllegalArgumentException("Initial balance must be at least " + MIN_BET);
        }
        this.balance = initialBalance;
        this.currentBet = 0.0;
    }
    
    /**
     * Places a bet
     * @param amount amount to bet
     * @throws InvalidBetException if bet is invalid
     */
    public void placeBet(double amount) throws InvalidBetException {
        validateBet(amount);
        
        if (amount > balance) {
            throw new InvalidBetException("Insufficient funds. Balance: $" + balance + ", Bet: $" + amount);
        }
        
        this.currentBet = amount;
        this.balance -= amount;
    }
    
    /**
     * Wins the bet - receives double the bet amount
     */
    public void winBet() {
        balance += currentBet * 2;
        currentBet = 0;
    }
    
    /**
     * Wins blackjack - receives 2.5x the bet amount
     */
    public void winBlackjack() {
        balance += currentBet * 2.5;
        currentBet = 0;
    }
    
    /**
     * Loses the bet - no refund
     */
    public void loseBet() {
        currentBet = 0;
    }
    
    /**
     * Push (tie) - gets bet back
     */
    public void pushBet() {
        balance += currentBet;
        currentBet = 0;
    }
    
    /**
     * Validates bet amount
     * @param amount bet amount to validate
     * @throws InvalidBetException if bet is invalid
     */
    private void validateBet(double amount) throws InvalidBetException {
        if (amount < MIN_BET) {
            throw new InvalidBetException("Minimum bet is $" + MIN_BET);
        }
        
        if (amount > MAX_BET) {
            throw new InvalidBetException("Maximum bet is $" + MAX_BET);
        }
        
        if (amount <= 0) {
            throw new InvalidBetException("Bet must be positive");
        }
    }
    
    // Getters
    public double getBalance() { return balance; }
    public double getCurrentBet() { return currentBet; }
    public double getMinBet() { return MIN_BET; }
    public double getMaxBet() { return MAX_BET; }
    
    /**
     * Checks if player has sufficient funds for minimum bet
     * @return true if player can afford minimum bet
     */
    public boolean canAffordMinBet() {
        return balance >= MIN_BET;
    }
    
    /**
     * Adds funds to bankroll
     * @param amount amount to add
     */
    public void addFunds(double amount) {
        if (amount > 0) {
            balance += amount;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Balance: $%.2f, Current Bet: $%.2f", balance, currentBet);
    }
}
