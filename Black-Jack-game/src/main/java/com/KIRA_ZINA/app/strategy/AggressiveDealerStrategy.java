package com.KIRA_ZINA.app.strategy;

import com.KIRA_ZINA.app.model.Deck;

/**
 * Aggressive dealer strategy - takes more risks
 * Hits more often, tries to get closer to 21
 */
public class AggressiveDealerStrategy implements DealerStrategy {
    
    @Override
    public boolean shouldHit(int handValue, Deck deck) {
        // Aggressive approach: hit more often
        if (handValue >= 18) {
            return false; // Still respect basic limits
        }
        
        if (handValue <= 11) {
            return true; // Always hit on 11 or less
        }
        
        // More aggressive on 12-17
        if (handValue == 12) {
            return true; // Aggressive - always hit on 12
        }
        
        if (handValue >= 13 && handValue <= 17) {
            // Consider deck composition for aggressive play
            if (deck != null && deck.size() > 0) {
                double highCardRatio = estimateHighCardRatio(deck);
                // Be more aggressive when deck has more low cards
                return highCardRatio < 0.4; // Hit unless very high card ratio
            }
            return true; // Default to aggressive
        }
        
        return true;
    }
    
    private double estimateHighCardRatio(Deck deck) {
        // Simplified estimation
        return 0.3;
    }
    
    @Override
    public String getStrategyName() {
        return "Aggressive";
    }
}
