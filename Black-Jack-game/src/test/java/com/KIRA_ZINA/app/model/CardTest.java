package com.KIRA_ZINA.app.model;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Card class
 */
public class CardTest {
    
    @Test
    public void testCardCreation() {
        Card card = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        
        assertEquals(Card.Suit.HEARTS, card.getSuit());
        assertEquals(Card.Rank.ACE, card.getRank());
        assertEquals(1, card.getValue());
    }
    
    @Test
    public void testCardValues() {
        // Test number cards
        assertEquals(2, new Card(Card.Suit.HEARTS, Card.Rank.TWO).getValue());
        assertEquals(3, new Card(Card.Suit.HEARTS, Card.Rank.THREE).getValue());
        assertEquals(4, new Card(Card.Suit.HEARTS, Card.Rank.FOUR).getValue());
        assertEquals(5, new Card(Card.Suit.HEARTS, Card.Rank.FIVE).getValue());
        assertEquals(6, new Card(Card.Suit.HEARTS, Card.Rank.SIX).getValue());
        assertEquals(7, new Card(Card.Suit.HEARTS, Card.Rank.SEVEN).getValue());
        assertEquals(8, new Card(Card.Suit.HEARTS, Card.Rank.EIGHT).getValue());
        assertEquals(9, new Card(Card.Suit.HEARTS, Card.Rank.NINE).getValue());
        assertEquals(10, new Card(Card.Suit.HEARTS, Card.Rank.TEN).getValue());
        
        // Test face cards
        assertEquals(10, new Card(Card.Suit.HEARTS, Card.Rank.JACK).getValue());
        assertEquals(10, new Card(Card.Suit.HEARTS, Card.Rank.QUEEN).getValue());
        assertEquals(10, new Card(Card.Suit.HEARTS, Card.Rank.KING).getValue());
        
        // Test Ace
        assertEquals(1, new Card(Card.Suit.HEARTS, Card.Rank.ACE).getValue());
    }
    
    @Test
    public void testCardToString() {
        Card card = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        String cardString = card.toString();
        
        assertNotNull(cardString);
        assertTrue(cardString.contains("ACE"));
        assertTrue(cardString.contains("HEARTS"));
    }
    
    @Test
    public void testDifferentSuits() {
        Card heartsCard = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        Card spadesCard = new Card(Card.Suit.SPADES, Card.Rank.ACE);
        Card diamondsCard = new Card(Card.Suit.DIAMONDS, Card.Rank.ACE);
        Card clubsCard = new Card(Card.Suit.CLUBS, Card.Rank.ACE);
        
        assertEquals(Card.Suit.HEARTS, heartsCard.getSuit());
        assertEquals(Card.Suit.SPADES, spadesCard.getSuit());
        assertEquals(Card.Suit.DIAMONDS, diamondsCard.getSuit());
        assertEquals(Card.Suit.CLUBS, clubsCard.getSuit());
        
        // All should have same value
        assertEquals(1, heartsCard.getValue());
        assertEquals(1, spadesCard.getValue());
        assertEquals(1, diamondsCard.getValue());
        assertEquals(1, clubsCard.getValue());
    }
    
    @Test
    public void testDifferentRanks() {
        Card twoCard = new Card(Card.Suit.HEARTS, Card.Rank.TWO);
        Card jackCard = new Card(Card.Suit.HEARTS, Card.Rank.JACK);
        Card aceCard = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        
        assertEquals(Card.Rank.TWO, twoCard.getRank());
        assertEquals(Card.Rank.JACK, jackCard.getRank());
        assertEquals(Card.Rank.ACE, aceCard.getRank());
        
        assertEquals(2, twoCard.getValue());
        assertEquals(10, jackCard.getValue());
        assertEquals(1, aceCard.getValue());
    }
    
    @Test
    public void testCardEquality() {
        Card card1 = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        Card card2 = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        Card card3 = new Card(Card.Suit.SPADES, Card.Rank.ACE);
        
        assertEquals(card1, card2);
        assertNotEquals(card1, card3);
        
        // Test equals with null and different class
        assertNotEquals(card1, null);
        assertNotEquals(card1, "not a card");
    }
    
    @Test
    public void testCardHashCode() {
        Card card1 = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        Card card2 = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        Card card3 = new Card(Card.Suit.SPADES, Card.Rank.ACE);
        
        assertEquals(card1.hashCode(), card2.hashCode());
        // Hash codes might be different for different cards (but not guaranteed)
    }
    
    @Test
    public void testAllCardCombinations() {
        // Test that we can create all possible card combinations
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                Card card = new Card(suit, rank);
                assertNotNull(card);
                assertEquals(suit, card.getSuit());
                assertEquals(rank, card.getRank());
                assertTrue(card.getValue() >= 1 && card.getValue() <= 10);
            }
        }
    }
    
    @Test
    public void testSuitEnum() {
        assertEquals(4, Card.Suit.values().length);
        
        // Test that all suits exist
        assertNotNull(Card.Suit.HEARTS);
        assertNotNull(Card.Suit.SPADES);
        assertNotNull(Card.Suit.DIAMONDS);
        assertNotNull(Card.Suit.CLUBS);
    }
    
    @Test
    public void testRankEnum() {
        assertEquals(13, Card.Rank.values().length);
        
        // Test that all ranks exist
        assertNotNull(Card.Rank.ACE);
        assertNotNull(Card.Rank.TWO);
        assertNotNull(Card.Rank.THREE);
        assertNotNull(Card.Rank.FOUR);
        assertNotNull(Card.Rank.FIVE);
        assertNotNull(Card.Rank.SIX);
        assertNotNull(Card.Rank.SEVEN);
        assertNotNull(Card.Rank.EIGHT);
        assertNotNull(Card.Rank.NINE);
        assertNotNull(Card.Rank.TEN);
        assertNotNull(Card.Rank.JACK);
        assertNotNull(Card.Rank.QUEEN);
        assertNotNull(Card.Rank.KING);
    }
}
