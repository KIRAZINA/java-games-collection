package com.KIRA_ZINA.app.model;

import java.util.*;

/**
 * Represents a deck of 52 playing cards
 */
public class Deck {
    private List<Card> cards;

    public Deck() {
        initializeDeck();
    }

    /**
     * Initializes the deck with 52 unique cards
     */
    private void initializeDeck() {
        cards = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }

    /**
     * Shuffles the deck using Collections.shuffle
     */
    public void shuffle() {
        Collections.shuffle(cards);
    }

    /**
     * Deals a card from the top of the deck
     * @return the top card from the deck
     */
    public Card dealCard() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Cannot deal from an empty deck");
        }
        
        return cards.remove(cards.size() - 1);
    }

    /**
     * Returns the number of cards remaining in the deck
     */
    public int size() {
        return cards.size();
    }
}