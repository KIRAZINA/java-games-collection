package com.KIRA_ZINA.app.model;

import java.util.*;

/**
 * Represents a hand of cards for a player or dealer
 */
public class Hand {
    private List<Card> cards;

    public Hand() {
        cards = new ArrayList<>();
    }

    /**
     * Adds a card to the hand
     */
    public void addCard(Card card) {
        cards.add(card);
    }

    /**
     * Calculates the value of the hand, handling aces appropriately
     * @return the calculated value of the hand
     */
    public int getValue() {
        int value = 0;
        int aces = 0;

        // Calculate value of non-ace cards first
        for (Card card : cards) {
            if (card.getRank() == Card.Rank.ACE) {
                aces++;
            } else {
                value += card.getValue();
            }
        }

        // Handle aces - add as 11 if possible without busting, otherwise as 1
        for (int i = 0; i < aces; i++) {
            if (value + 11 <= 21) {
                value += 11;  // Count ace as 11
            } else {
                value += 1;   // Count ace as 1
            }
        }

        return value;
    }

    /**
     * Checks if the hand has a blackjack (21 with only 2 cards: Ace + 10-value card)
     * @return true if the hand is a blackjack, false otherwise
     */
    public boolean isBlackjack() {
        return cards.size() == 2 && getValue() == 21;
    }

    /**
     * Checks if the hand is busted (value over 21)
     * @return true if the hand is busted, false otherwise
     */
    public boolean isBust() {
        return getValue() > 21;
    }

    /**
     * Returns the list of cards in the hand
     */
    public List<Card> getCards() {
        return cards;
    }

    /**
     * Clears the hand
     */
    public void clear() {
        cards.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cards.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(cards.get(i));
        }
        return sb.toString();
    }
}