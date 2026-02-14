package com.KIRA_ZINA.app.entity;

import com.KIRA_ZINA.app.model.Card;
import com.KIRA_ZINA.app.model.Deck;
import com.KIRA_ZINA.app.strategy.BasicDealerStrategy;
import com.KIRA_ZINA.app.strategy.ConservativeDealerStrategy;
import com.KIRA_ZINA.app.strategy.AggressiveDealerStrategy;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Dealer class
 */
public class DealerTest {
    
    private Dealer dealer;
    private Deck mockDeck;
    
    @Before
    public void setUp() {
        dealer = new Dealer();
        mockDeck = new Deck();
        dealer.setStrategy(new BasicDealerStrategy());
    }
    
    @Test
    public void testInitialDealerState() {
        assertNotNull(dealer.getHand());
        assertEquals(0, dealer.getHand().getCards().size());
        assertEquals(0, dealer.getHandValue());
        assertFalse(dealer.isBust());
        assertFalse(dealer.hasBlackjack());
    }
    
    @Test
    public void testSetStrategy() {
        ConservativeDealerStrategy conservative = new ConservativeDealerStrategy();
        dealer.setStrategy(conservative);
        dealer.setDeck(mockDeck);
        
        // Strategy should be set (we can't directly access it, but shouldHit should work)
        assertTrue(dealer.shouldHit()); // With empty hand, should hit
    }
    
    @Test(expected = IllegalStateException.class)
    public void testShouldHitWithoutStrategy() {
        Dealer dealerWithoutStrategy = new Dealer();
        dealerWithoutStrategy.shouldHit();
    }
    
    @Test
    public void testShouldHitWithBasicStrategy() {
        dealer.setStrategy(new BasicDealerStrategy());
        dealer.setDeck(mockDeck);
        
        // Add cards for value 16 - should hit
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.SIX));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        assertTrue(dealer.shouldHit());
        
        // Add card for value 17 - should stand
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        assertFalse(dealer.shouldHit());
    }
    
    @Test
    public void testShouldHitWithConservativeStrategy() {
        dealer.setStrategy(new ConservativeDealerStrategy());
        dealer.setDeck(mockDeck);
        
        // Add cards for value 11 - should hit
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.SIX));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));
        assertTrue(dealer.shouldHit());
        
        // Add card for value 12 - should stand (conservative)
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        assertFalse(dealer.shouldHit());
    }
    
    @Test
    public void testShouldHitWithAggressiveStrategy() {
        dealer.setStrategy(new AggressiveDealerStrategy());
        dealer.setDeck(mockDeck);
        
        // Add cards for value 17 - should hit (aggressive)
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.SEVEN));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        assertTrue(dealer.shouldHit());
        
        // Add card for value 18 - should stand
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        assertFalse(dealer.shouldHit());
    }
    
    @Test
    public void testSetDeck() {
        Deck deck = new Deck();
        dealer.setDeck(deck);
        dealer.setStrategy(new BasicDealerStrategy());
        
        // Deck should be set (we can't directly access it, but it should work with shouldHit)
        assertTrue(dealer.shouldHit()); // With empty hand, should hit
    }
    
    @Test
    public void testAddCardToHand() {
        Card card = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        dealer.addCardToHand(card);
        
        assertEquals(1, dealer.getHand().getCards().size());
        assertEquals(11, dealer.getHandValue());
        assertFalse(dealer.isBust());
        assertFalse(dealer.hasBlackjack());
    }
    
    @Test
    public void testClearHand() {
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        
        assertEquals(2, dealer.getHand().getCards().size());
        
        dealer.clearHand();
        
        assertEquals(0, dealer.getHand().getCards().size());
        assertEquals(0, dealer.getHandValue());
        assertFalse(dealer.isBust());
        assertFalse(dealer.hasBlackjack());
    }
    
    @Test
    public void testGetHand() {
        assertNotNull(dealer.getHand());
        assertSame(dealer.getHand(), dealer.getHand()); // Should return same reference
    }
    
    @Test
    public void testGetHandValue() {
        assertEquals(0, dealer.getHandValue());
        
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.EIGHT));
        assertEquals(8, dealer.getHandValue());
        
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.NINE));
        assertEquals(17, dealer.getHandValue());
    }
    
    @Test
    public void testIsBust() {
        assertFalse(dealer.isBust());
        
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.JACK));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.QUEEN));
        
        assertTrue(dealer.isBust());
    }
    
    @Test
    public void testHasBlackjack() {
        assertFalse(dealer.hasBlackjack());
        
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        
        assertTrue(dealer.hasBlackjack());
    }
    
    @Test
    public void testStrategyBehaviorConsistency() {
        dealer.setStrategy(new BasicDealerStrategy());
        dealer.setDeck(mockDeck);
        
        // Test consistent behavior with same hand value
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.EIGHT));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.NINE)); // Value: 17
        
        // Should always return false for value 17 with basic strategy
        assertFalse(dealer.shouldHit());
        assertFalse(dealer.shouldHit());
        assertFalse(dealer.shouldHit());
    }
    
    @Test
    public void testDealerWithBustHand() {
        dealer.setStrategy(new BasicDealerStrategy());
        dealer.setDeck(mockDeck);
        
        // Make dealer bust
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.JACK));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.QUEEN));
        
        assertTrue(dealer.isBust());
        // Even with bust hand, shouldHit should still work (though logically dealer wouldn't hit)
        assertFalse(dealer.shouldHit()); // Basic strategy: 21+ stands
    }
    
    @Test
    public void testDealerWithBlackjack() {
        dealer.setStrategy(new BasicDealerStrategy());
        dealer.setDeck(mockDeck);
        
        // Give dealer blackjack
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        
        assertTrue(dealer.hasBlackjack());
        assertEquals(21, dealer.getHandValue());
        assertFalse(dealer.shouldHit()); // Should stand on blackjack
    }

    @Test
    public void testVisibleCardWhenHandEmpty() {
        assertNull(dealer.getVisibleCard());
        assertEquals(0, dealer.getVisibleCardValue());
    }

    @Test
    public void testVisibleCardWhenCardsPresent() {
        Card first = new Card(Card.Suit.DIAMONDS, Card.Rank.FIVE);
        dealer.addCardToHand(first);
        dealer.addCardToHand(new Card(Card.Suit.SPADES, Card.Rank.KING));

        assertEquals(first, dealer.getVisibleCard());
        assertEquals(5, dealer.getVisibleCardValue());
    }
}
