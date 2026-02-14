package com.KIRA_ZINA.app.entity;

import com.KIRA_ZINA.app.model.Card;
import com.KIRA_ZINA.app.model.Deck;
import com.KIRA_ZINA.app.strategy.DealerStrategy;

/**
 * Represents the dealer in the blackjack game with strategy pattern
 */
public class Dealer extends Player {
    private Deck deck;
    private DealerStrategy strategy;
    
    public Dealer() {
        super();
    }
    
    public void setDeck(Deck deck) {
        this.deck = deck;
    }
    
    public void setStrategy(DealerStrategy strategy) {
        this.strategy = strategy;
    }
    
    /**
     * Determines if the dealer should hit based on current strategy
     * @return true if the dealer should hit, false otherwise
     */
    public boolean shouldHit() {
        if (strategy == null) {
            throw new IllegalStateException("Dealer strategy not set");
        }
        return strategy.shouldHit(getHandValue(), deck);
    }

    /**
     * Gets the value of the dealer's hand showing only the first card (for the player's view)
     */
    public int getVisibleCardValue() {
        if (hand.getCards().isEmpty()) {
            return 0;
        }
        Card visibleCard = hand.getCards().get(0);
        return visibleCard.getValue();
    }

    /**
     * Gets the visible card (first card) in the dealer's hand
     */
    public Card getVisibleCard() {
        if (hand.getCards().isEmpty()) {
            return null;
        }
        return hand.getCards().get(0);
    }
}