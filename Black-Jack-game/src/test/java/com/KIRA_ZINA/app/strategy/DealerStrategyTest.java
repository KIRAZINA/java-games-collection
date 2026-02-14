package com.KIRA_ZINA.app.strategy;

import com.KIRA_ZINA.app.model.Deck;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for dealer strategies
 */
public class DealerStrategyTest {
    
    private Deck mockDeck;
    
    @Before
    public void setUp() {
        mockDeck = new Deck(); // Mock deck for testing
    }
    
    @Test
    public void testBasicStrategy() {
        DealerStrategy strategy = new BasicDealerStrategy();
        
        // Should hit on 16 or less
        assertTrue(strategy.shouldHit(16, mockDeck));
        assertTrue(strategy.shouldHit(15, mockDeck));
        assertTrue(strategy.shouldHit(11, mockDeck));
        
        // Should stand on 17 or more
        assertFalse(strategy.shouldHit(17, mockDeck));
        assertFalse(strategy.shouldHit(18, mockDeck));
        assertFalse(strategy.shouldHit(20, mockDeck));
        
        assertEquals("Basic", strategy.getStrategyName());
    }
    
    @Test
    public void testConservativeStrategy() {
        DealerStrategy strategy = new ConservativeDealerStrategy();
        
        // Should hit on 11 or less
        assertTrue(strategy.shouldHit(11, mockDeck));
        assertTrue(strategy.shouldHit(10, mockDeck));
        
        // Should stand on 12 or more (conservative)
        assertFalse(strategy.shouldHit(12, mockDeck));
        assertFalse(strategy.shouldHit(16, mockDeck));
        assertFalse(strategy.shouldHit(17, mockDeck));
        
        assertEquals("Conservative", strategy.getStrategyName());
    }
    
    @Test
    public void testAggressiveStrategy() {
        DealerStrategy strategy = new AggressiveDealerStrategy();
        
        // Should hit on 17 or less (aggressive)
        assertTrue(strategy.shouldHit(17, mockDeck));
        assertTrue(strategy.shouldHit(16, mockDeck));
        assertTrue(strategy.shouldHit(12, mockDeck));
        assertTrue(strategy.shouldHit(11, mockDeck));
        
        // Should stand on 18 or more
        assertFalse(strategy.shouldHit(18, mockDeck));
        assertFalse(strategy.shouldHit(19, mockDeck));
        assertFalse(strategy.shouldHit(20, mockDeck));
        
        assertEquals("Aggressive", strategy.getStrategyName());
    }
    
    @Test
    public void testStrategyConsistency() {
        DealerStrategy basic = new BasicDealerStrategy();
        DealerStrategy conservative = new ConservativeDealerStrategy();
        DealerStrategy aggressive = new AggressiveDealerStrategy();
        
        // Test same hand value with different strategies
        int handValue = 15;
        
        boolean basicHits = basic.shouldHit(handValue, mockDeck);
        boolean conservativeHits = conservative.shouldHit(handValue, mockDeck);
        boolean aggressiveHits = aggressive.shouldHit(handValue, mockDeck);
        
        // Conservative should be less likely to hit than basic
        assertTrue(basicHits || !conservativeHits);
        
        // Aggressive should be more likely to hit than basic
        assertTrue(aggressiveHits || !basicHits);
    }
}
