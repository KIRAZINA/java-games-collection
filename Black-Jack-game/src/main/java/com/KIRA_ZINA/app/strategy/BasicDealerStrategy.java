package com.KIRA_ZINA.app.strategy;

import com.KIRA_ZINA.app.model.Deck;

/**
 * Basic dealer strategy - follows standard casino rules
 * Hits until 17, stands on 17+
 */
public class BasicDealerStrategy implements DealerStrategy {
    
    @Override
    public boolean shouldHit(int handValue, Deck deck) {
        // Basic casino rules: hit until 17, stand on 17+
        return handValue < 17;
    }
    
    @Override
    public String getStrategyName() {
        return "Basic";
    }
}
