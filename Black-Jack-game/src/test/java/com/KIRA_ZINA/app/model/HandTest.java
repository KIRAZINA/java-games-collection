package com.KIRA_ZINA.app.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Hand class
 */
public class HandTest {
    
    private Hand hand;
    
    @Before
    public void setUp() {
        hand = new Hand();
    }
    
    @Test
    public void testInitialHandState() {
        assertEquals(0, hand.getCards().size());
        assertEquals(0, hand.getValue());
        assertFalse(hand.isBust());
        assertFalse(hand.isBlackjack());
    }
    
    @Test
    public void testAddCard() {
        Card card = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        hand.addCard(card);
        
        assertEquals(1, hand.getCards().size());
        assertEquals(11, hand.getValue()); // Ace counts as 11 initially
        assertFalse(hand.isBust());
        assertFalse(hand.isBlackjack());
    }
    
    @Test
    public void testAddMultipleCards() {
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.SEVEN));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));
        
        assertEquals(2, hand.getCards().size());
        assertEquals(12, hand.getValue());
        assertFalse(hand.isBust());
        assertFalse(hand.isBlackjack());
    }
    
    @Test
    public void testClearHand() {
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        
        assertEquals(2, hand.getCards().size());
        
        hand.clear();
        
        assertEquals(0, hand.getCards().size());
        assertEquals(0, hand.getValue());
        assertFalse(hand.isBust());
        assertFalse(hand.isBlackjack());
    }
    
    @Test
    public void testGetCards() {
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        
        assertEquals(2, hand.getCards().size());
        assertEquals(2, hand.getCards().size()); // Should return same list
    }
    
    @Test
    public void testGetValue() {
        assertEquals(0, hand.getValue());
        
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.SEVEN));
        assertEquals(7, hand.getValue());
        
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));
        assertEquals(12, hand.getValue());
    }
    
    @Test
    public void testIsBust() {
        assertFalse(hand.isBust());
        
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.JACK));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.QUEEN));
        
        assertTrue(hand.isBust());
    }
    
    @Test
    public void testHasBlackjack() {
        assertFalse(hand.isBlackjack());
        
        // Add blackjack combination
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        
        assertTrue(hand.isBlackjack());
    }
    
    @Test
    public void testHasBlackjackWithMoreThanTwoCards() {
        // Three cards totaling 21 should not be blackjack
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.SEVEN));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.NINE));
        
        assertEquals(21, hand.getValue());
        assertFalse(hand.isBlackjack());
    }
    
    @Test
    public void testAceValueAdjustment() {
        // Test that Ace value adjusts correctly
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        assertEquals(11, hand.getValue());
        
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        assertEquals(21, hand.getValue()); // Ace still 11
        
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));
        assertEquals(16, hand.getValue()); // Ace becomes 1
        
        assertFalse(hand.isBust());
    }
    
    @Test
    public void testMultipleAces() {
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        hand.addCard(new Card(Card.Suit.SPADES, Card.Rank.ACE));
        assertEquals(12, hand.getValue()); // 11 + 1
        
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.NINE));
        assertEquals(21, hand.getValue()); // 11 + 1 + 9
        
        assertFalse(hand.isBust());
        assertFalse(hand.isBlackjack()); // More than 2 cards
    }
    
    @Test
    public void testHandToString() {
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        hand.addCard(new Card(Card.Suit.SPADES, Card.Rank.KING));
        
        String handString = hand.toString();
        assertNotNull(handString);
        assertTrue(handString.contains("ACE"));
        assertTrue(handString.contains("KING"));
    }
    
    @Test
    public void testEmptyHandToString() {
        String handString = hand.toString();
        assertNotNull(handString);
    }
    
    @Test
    public void testComplexHandValue() {
        // Test complex scenario with multiple cards and aces
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));     // 11
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));     // 5  -> 16
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));     // 1  -> 17
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.THREE));    // 3  -> 20
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.TWO));      // 2  -> 22
        
        assertEquals(22, hand.getValue());
        assertTrue(hand.isBust());
    }
    
    @Test
    public void testHandWithFaceCards() {
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.JACK));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.QUEEN));
        
        assertEquals(20, hand.getValue());
        assertFalse(hand.isBust());
        assertFalse(hand.isBlackjack());
    }
    
    @Test
    public void testHandWithAllSameSuit() {
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.TWO));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.THREE));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.FOUR));
        
        assertEquals(9, hand.getValue());
        assertFalse(hand.isBust());
        assertFalse(hand.isBlackjack());
    }
    
    @Test
    public void testHandValueBoundary() {
        // Test exactly 21
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.JACK));
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        
        assertEquals(21, hand.getValue());
        assertFalse(hand.isBust());
        assertFalse(hand.isBlackjack()); // 3 cards
        
        // Test just over 21
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.TWO));
        
        assertEquals(23, hand.getValue()); // Ace becomes 1, but still bust: 10+10+1+2 = 23
        assertTrue(hand.isBust());
    }
}
