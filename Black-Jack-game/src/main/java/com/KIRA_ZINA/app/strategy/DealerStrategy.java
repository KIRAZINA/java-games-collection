package com.KIRA_ZINA.app.strategy;

import com.KIRA_ZINA.app.model.Deck;

/**
 * Strategy interface for different dealer AI approaches
 */
public interface DealerStrategy {
    
    /**
     * Determines if the dealer should hit based on the current strategy
     * @param handValue current value of dealer's hand
     * @param deck reference to deck for card counting (if needed)
     * @return true if dealer should hit, false otherwise
     */
    boolean shouldHit(int handValue, Deck deck);
    
    /**
     * Gets the name of this strategy
     * @return strategy name
     */
    String getStrategyName();
}
