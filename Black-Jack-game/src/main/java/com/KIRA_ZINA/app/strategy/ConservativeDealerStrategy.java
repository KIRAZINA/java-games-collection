package com.KIRA_ZINA.app.strategy;

import com.KIRA_ZINA.app.model.Deck;

/**
 * Conservative dealer strategy - more cautious approach
 * Stands on lower values, avoids risky hits
 */
public class ConservativeDealerStrategy implements DealerStrategy {
    
    @Override
    public boolean shouldHit(int handValue, Deck deck) {
        // Conservative approach: stand earlier
        if (handValue >= 16) {
            return false;
        }
        
        if (handValue <= 11) {
            return true; // Always hit on 11 or less (can't bust)
        }
        
        // Conservative on 12-15
        if (handValue == 12) {
            return false; // Very conservative - stand on 12
        }
        
        if (handValue >= 13 && handValue <= 15) {
            // Consider deck composition
            if (deck != null && deck.size() > 0) {
                // Be more conservative when deck has many high cards
                double highCardRatio = estimateHighCardRatio(deck);
                return highCardRatio < 0.3; // Only hit if low cards dominate
            }
            return false; // Default to conservative
        }
        
        return true;
    }
    
    private double estimateHighCardRatio(Deck deck) {
        // Simplified estimation - assume ~30% high cards normally
        return 0.3;
    }
    
    @Override
    public String getStrategyName() {
        return "Conservative";
    }
}
