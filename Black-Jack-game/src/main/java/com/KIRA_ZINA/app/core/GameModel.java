package com.KIRA_ZINA.app.core;

import com.KIRA_ZINA.app.model.Deck;
import com.KIRA_ZINA.app.model.Card;
import com.KIRA_ZINA.app.model.Bankroll;
import com.KIRA_ZINA.app.entity.Player;
import com.KIRA_ZINA.app.entity.Dealer;
import com.KIRA_ZINA.app.strategy.DealerStrategy;
import com.KIRA_ZINA.app.exception.GameException;
import com.KIRA_ZINA.app.exception.InvalidCardOperationException;
import com.KIRA_ZINA.app.exception.InvalidBetException;

/**
 * Game model - manages the state and data of the blackjack game
 */
public class GameModel {
    private Deck deck;
    private Player player;
    private Dealer dealer;
    private DealerStrategy dealerStrategy;
    private Bankroll bankroll;
    private boolean gameOver;
    private String winner;
    private boolean bettingPhase;
    private boolean roundInitialized;
    
    public GameModel(double initialBalance) {
        this.deck = new Deck();
        this.player = new Player();
        this.dealer = new Dealer();
        this.bankroll = new Bankroll(initialBalance);
        this.gameOver = false;
        this.bettingPhase = true;
        this.roundInitialized = false;
    }
    
    /**
     * Initializes a new round
     * @throws GameException if initialization fails
     */
    public void initializeRound() throws GameException {
        try {
            // Reset hands
            player.clearHand();
            dealer.clearHand();
            
            // Reset betting phase
            bettingPhase = true;
            
            // Recreate the deck when there are not enough cards for a full round.
            if (deck.size() < 4) {
                deck = new Deck();
            }

            // Shuffle deck
            deck.shuffle();
            
            // Set deck reference for dealer
            dealer.setDeck(deck);
            
            // Reset game state
            gameOver = false;
            winner = null;
            roundInitialized = true;
            
        } catch (Exception e) {
            throw new GameException("Failed to initialize round: " + e.getMessage(), e);
        }
    }
    
    /**
     * Places a bet for the current round
     * @param amount bet amount
     * @throws InvalidBetException if bet is invalid
     * @throws GameException if dealing cards fails
     */
    public void placeBet(double amount) throws InvalidBetException, GameException {
        if (!roundInitialized) {
            throw new GameException("Round is not initialized. Call initializeRound() first.");
        }

        if (!bettingPhase) {
            throw new IllegalStateException("Cannot place bet after betting phase has ended");
        }
        
        bankroll.placeBet(amount);
        bettingPhase = false;
        
        // Deal initial cards after bet is placed
        try {
            dealInitialCards();
        } catch (InvalidCardOperationException e) {
            throw new GameException("Failed to deal initial cards after bet: " + e.getMessage(), e);
        }
    }
    
    /**
     * Deals initial two cards to player and dealer
     * @throws InvalidCardOperationException if dealing fails
     */
    private void dealInitialCards() throws InvalidCardOperationException {
        try {
            // Deal two cards to player
            player.addCardToHand(deck.dealCard());
            player.addCardToHand(deck.dealCard());
            
            // Deal two cards to dealer
            dealer.addCardToHand(deck.dealCard());
            dealer.addCardToHand(deck.dealCard());
        } catch (Exception e) {
            throw new InvalidCardOperationException("Failed to deal initial cards: " + e.getMessage(), e);
        }
    }
    
    /**
     * Player takes a card
     * @throws InvalidCardOperationException if dealing fails
     */
    public void playerHit() throws InvalidCardOperationException {
        try {
            player.addCardToHand(deck.dealCard());
        } catch (Exception e) {
            throw new InvalidCardOperationException("Failed to deal card to player: " + e.getMessage(), e);
        }
    }
    
    /**
     * Dealer takes a card
     * @throws InvalidCardOperationException if dealing fails
     */
    public void dealerHit() throws InvalidCardOperationException {
        try {
            dealer.addCardToHand(deck.dealCard());
        } catch (Exception e) {
            throw new InvalidCardOperationException("Failed to deal card to dealer: " + e.getMessage(), e);
        }
    }
    
    /**
     * Determines if dealer should hit based on current strategy
     * @return true if dealer should hit
     */
    public boolean shouldDealerHit() {
        if (dealerStrategy == null) {
            throw new IllegalStateException("Dealer strategy not set");
        }
        return dealerStrategy.shouldHit(dealer.getHandValue(), deck);
    }
    
    /**
     * Determines the winner of the round and settles bets
     */
    public void determineWinner() {
        // Reset winner
        winner = null;
        
        int playerValue = player.getHandValue();
        int dealerValue = dealer.getHandValue();
        
        boolean playerHasBlackjack = player.hasBlackjack();
        boolean dealerHasBlackjack = dealer.hasBlackjack();
        boolean playerBust = player.isBust();
        boolean dealerBust = dealer.isBust();
        
        // Check for blackjacks first
        if (playerHasBlackjack && !dealerHasBlackjack) {
            winner = "player";
            bankroll.winBlackjack();
        } else if (dealerHasBlackjack && !playerHasBlackjack) {
            winner = "dealer";
            bankroll.loseBet();
        } else if (playerHasBlackjack && dealerHasBlackjack) {
            winner = "tie";
            bankroll.pushBet();
        }
        // Check if player busted
        else if (playerBust) {
            winner = "dealer";
            bankroll.loseBet();
        }
        // Check if dealer busted
        else if (dealerBust) {
            winner = "player";
            bankroll.winBet();
        }
        // Compare values (only if neither busted)
        else if (playerValue > dealerValue) {
            winner = "player";
            bankroll.winBet();
        } else if (dealerValue > playerValue) {
            winner = "dealer";
            bankroll.loseBet();
        } else { // Values are equal
            winner = "tie";
            bankroll.pushBet();
        }
        
        gameOver = true;
    }
    
    /**
     * Checks if player can continue playing
     * @return true if player has sufficient funds
     */
    public boolean canContinuePlaying() {
        return bankroll.canAffordMinBet();
    }
    
    // Getters and setters
    public Deck getDeck() { return deck; }
    public Player getPlayer() { return player; }
    public Dealer getDealer() { return dealer; }
    public DealerStrategy getDealerStrategy() { return dealerStrategy; }
    public void setDealerStrategy(DealerStrategy dealerStrategy) { this.dealerStrategy = dealerStrategy; }
    public boolean isGameOver() { return gameOver; }
    public String getWinner() { return winner; }
    public Bankroll getBankroll() { return bankroll; }
    public boolean isBettingPhase() { return bettingPhase; }
}
