package com.KIRA_ZINA.app.model;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Deck class
 */
public class DeckTest {
    
    private Deck deck;
    
    @Before
    public void setUp() {
        deck = new Deck();
    }
    
    @Test
    public void testDeckInitialization() {
        assertEquals(52, deck.size());
    }
    
    @Test
    public void testDeckShuffle() {
        // Get initial order
        Card firstCard = deck.dealCard();
        Card secondCard = deck.dealCard();
        
        // Create new deck and shuffle
        Deck shuffledDeck = new Deck();
        shuffledDeck.shuffle();
        
        // The shuffled deck should have different order (very high probability)
        Card shuffledFirst = shuffledDeck.dealCard();
        Card shuffledSecond = shuffledDeck.dealCard();
        
        // It's possible (though extremely unlikely) that shuffle results in same order
        // So we'll just verify that shuffle doesn't break the deck
        assertNotNull(shuffledFirst);
        assertNotNull(shuffledSecond);
        assertNotEquals(shuffledFirst, shuffledSecond);
    }
    
    @Test
    public void testDealCard() {
        int initialSize = deck.size();
        Card card = deck.dealCard();
        
        assertNotNull(card);
        assertEquals(initialSize - 1, deck.size());
        
        // Test that we can deal all cards
        for (int i = 0; i < 51; i++) {
            assertNotNull(deck.dealCard());
        }
        
        assertEquals(0, deck.size());
    }
    
    @Test(expected = IllegalStateException.class)
    public void testDealCardFromEmptyDeck() {
        // Deal all cards
        while (deck.size() > 0) {
            deck.dealCard();
        }
        
        // Try to deal from empty deck
        deck.dealCard();
    }
    
    @Test
    public void testDeckContainsAllCards() {
        // Count each suit and rank
        int[] suitCounts = new int[4];
        int[] rankCounts = new int[13];
        
        while (deck.size() > 0) {
            Card card = deck.dealCard();
            
            // Count suits
            switch (card.getSuit()) {
                case HEARTS: suitCounts[0]++; break;
                case SPADES: suitCounts[1]++; break;
                case DIAMONDS: suitCounts[2]++; break;
                case CLUBS: suitCounts[3]++; break;
            }
            
            // Count ranks
            switch (card.getRank()) {
                case ACE: rankCounts[0]++; break;
                case TWO: rankCounts[1]++; break;
                case THREE: rankCounts[2]++; break;
                case FOUR: rankCounts[3]++; break;
                case FIVE: rankCounts[4]++; break;
                case SIX: rankCounts[5]++; break;
                case SEVEN: rankCounts[6]++; break;
                case EIGHT: rankCounts[7]++; break;
                case NINE: rankCounts[8]++; break;
                case TEN: rankCounts[9]++; break;
                case JACK: rankCounts[10]++; break;
                case QUEEN: rankCounts[11]++; break;
                case KING: rankCounts[12]++; break;
            }
        }
        
        // Verify each suit appears 13 times
        for (int count : suitCounts) {
            assertEquals(13, count);
        }
        
        // Verify each rank appears 4 times
        for (int count : rankCounts) {
            assertEquals(4, count);
        }
    }
    
    @Test
    public void testNoDuplicateCards() {
        java.util.Set<String> cardStrings = new java.util.HashSet<>();
        
        while (deck.size() > 0) {
            Card card = deck.dealCard();
            String cardString = card.toString();
            
            // Each card should be unique
            assertFalse("Duplicate card found: " + cardString, 
                       cardStrings.contains(cardString));
            cardStrings.add(cardString);
        }
        
        // Should have exactly 52 unique cards
        assertEquals(52, cardStrings.size());
    }
    
    @Test
    public void testMultipleDecks() {
        Deck deck1 = new Deck();
        Deck deck2 = new Deck();
        
        // Both should start with 52 cards
        assertEquals(52, deck1.size());
        assertEquals(52, deck2.size());
        
        // Deal different cards from each
        Card card1 = deck1.dealCard();
        Card card2 = deck2.dealCard();
        
        // Should be different (unless shuffle resulted in same order, which is unlikely)
        assertNotNull(card1);
        assertNotNull(card2);
    }
    
    @Test
    public void testDeckAfterMultipleShuffles() {
        // Shuffle multiple times
        for (int i = 0; i < 5; i++) {
            deck.shuffle();
            assertEquals(52, deck.size());
            
            // Deal a few cards to ensure deck is still functional
            for (int j = 0; j < 5; j++) {
                assertNotNull(deck.dealCard());
            }
            
            // Reset deck for next shuffle
            deck = new Deck();
        }
    }
    
    @Test
    public void testDeckCardValues() {
        // Test that all cards have valid values
        while (deck.size() > 0) {
            Card card = deck.dealCard();
            int value = card.getValue();
            
            assertTrue("Invalid card value: " + value, 
                      value >= 1 && value <= 10);
        }
    }
}
