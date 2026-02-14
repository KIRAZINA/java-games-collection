package com.KIRA_ZINA.app.entity;

import com.KIRA_ZINA.app.model.Card;
import com.KIRA_ZINA.app.model.Hand;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Player class
 */
public class PlayerTest {
    
    private Player player;
    
    @Before
    public void setUp() {
        player = new Player();
    }
    
    @Test
    public void testInitialPlayerState() {
        assertNotNull(player.getHand());
        assertEquals(0, player.getHand().getCards().size());
        assertEquals(0, player.getHandValue());
        assertFalse(player.isBust());
        assertFalse(player.hasBlackjack());
    }
    
    @Test
    public void testAddCardToHand() {
        Card card = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        player.addCardToHand(card);
        
        assertEquals(1, player.getHand().getCards().size());
        assertEquals(11, player.getHandValue()); // Ace counts as 11 initially
        assertFalse(player.isBust());
        assertFalse(player.hasBlackjack());
    }
    
    @Test
    public void testClearHand() {
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        
        assertEquals(2, player.getHand().getCards().size());
        
        player.clearHand();
        
        assertEquals(0, player.getHand().getCards().size());
        assertEquals(0, player.getHandValue());
        assertFalse(player.isBust());
        assertFalse(player.hasBlackjack());
    }
    
    @Test
    public void testGetHand() {
        Hand hand = player.getHand();
        assertNotNull(hand);
        assertSame(hand, player.getHand()); // Should return same reference
    }
    
    @Test
    public void testGetHandValue() {
        assertEquals(0, player.getHandValue());
        
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.SEVEN));
        assertEquals(7, player.getHandValue());
        
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));
        assertEquals(12, player.getHandValue());
    }
    
    @Test
    public void testIsBust() {
        assertFalse(player.isBust());
        
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.JACK));
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.QUEEN));
        
        assertTrue(player.isBust());
    }
    
    @Test
    public void testHasBlackjack() {
        assertFalse(player.hasBlackjack());
        
        // Add blackjack combination
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        
        assertTrue(player.hasBlackjack());
    }
    
    @Test
    public void testHasBlackjackWithMoreThanTwoCards() {
        // Three cards totaling 21 should not be blackjack
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.SEVEN));
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.NINE));
        
        assertEquals(21, player.getHandValue());
        assertFalse(player.hasBlackjack());
    }
    
    @Test
    public void testAceValueAdjustment() {
        // Test that Ace value adjusts correctly
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        assertEquals(11, player.getHandValue());
        
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        assertEquals(21, player.getHandValue()); // Ace still 11
        
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));
        assertEquals(16, player.getHandValue()); // Ace becomes 1
        
        assertFalse(player.isBust());
    }
    
    @Test
    public void testMultipleAces() {
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        player.addCardToHand(new Card(Card.Suit.SPADES, Card.Rank.ACE));
        assertEquals(12, player.getHandValue()); // 11 + 1
        
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.NINE));
        assertEquals(21, player.getHandValue()); // 11 + 1 + 9
        
        assertFalse(player.isBust());
        assertFalse(player.hasBlackjack()); // More than 2 cards
    }
    
    @Test
    public void testHandToString() {
        player.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        player.addCardToHand(new Card(Card.Suit.SPADES, Card.Rank.KING));
        
        String handString = player.getHand().toString();
        assertNotNull(handString);
        assertTrue(handString.contains("ACE"));
        assertTrue(handString.contains("KING"));
    }
}
