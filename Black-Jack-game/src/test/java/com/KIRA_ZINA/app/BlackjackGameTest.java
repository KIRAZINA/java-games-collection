package com.KIRA_ZINA.app;

import org.junit.Test;
import static org.junit.Assert.*;
import com.KIRA_ZINA.app.model.*;
import com.KIRA_ZINA.app.entity.*;
import com.KIRA_ZINA.app.game.*;
import com.KIRA_ZINA.app.strategy.BasicDealerStrategy;

public class BlackjackGameTest {

    @Test
    public void testCardCreation() {
        Card card = new Card(Card.Suit.HEARTS, Card.Rank.ACE);
        assertEquals(Card.Suit.HEARTS, card.getSuit());
        assertEquals(Card.Rank.ACE, card.getRank());
        assertEquals(1, card.getValue()); // ACE has value 1 in our implementation
    }

    @Test
    public void testDeckInitialization() {
        Deck deck = new Deck();
        assertEquals(52, deck.size());
    }

    @Test
    public void testHandValueCalculation() {
        Hand hand = new Hand();
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        hand.addCard(new Card(Card.Suit.SPADES, Card.Rank.JACK));
        assertEquals(20, hand.getValue());
    }

    @Test
    public void testBlackjackDetection() {
        Hand hand = new Hand();
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        hand.addCard(new Card(Card.Suit.SPADES, Card.Rank.JACK));
        assertTrue(hand.isBlackjack());
    }

    @Test
    public void testBustDetection() {
        Hand hand = new Hand();
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.TEN));
        hand.addCard(new Card(Card.Suit.SPADES, Card.Rank.JACK));
        hand.addCard(new Card(Card.Suit.CLUBS, Card.Rank.THREE));
        assertTrue(hand.isBust());
    }

    @Test
    public void testAceValueCalculation() {
        Hand hand = new Hand();
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        hand.addCard(new Card(Card.Suit.SPADES, Card.Rank.NINE));
        assertEquals(20, hand.getValue()); // Ace should count as 11
    }

    @Test
    public void testAceAsOneWhenBust() {
        Hand hand = new Hand();
        hand.addCard(new Card(Card.Suit.HEARTS, Card.Rank.ACE));
        hand.addCard(new Card(Card.Suit.SPADES, Card.Rank.TEN));
        hand.addCard(new Card(Card.Suit.DIAMONDS, Card.Rank.SEVEN)); // Total would be 18 if Ace counts as 1
        assertEquals(18, hand.getValue());
    }

    @Test
    public void testDealerShouldHit() {
        Dealer dealer = new Dealer();
        dealer.setStrategy(new BasicDealerStrategy()); // Set strategy
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.FIVE));
        dealer.addCardToHand(new Card(Card.Suit.SPADES, Card.Rank.EIGHT));
        assertTrue(dealer.shouldHit()); // Value is 13, should hit
    }

    @Test
    public void testDealerShouldStand() {
        Dealer dealer = new Dealer();
        dealer.setStrategy(new BasicDealerStrategy()); // Set strategy
        dealer.addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.KING));
        dealer.addCardToHand(new Card(Card.Suit.SPADES, Card.Rank.SEVEN));
        assertFalse(dealer.shouldHit()); // Value is 17, should stand
    }
}