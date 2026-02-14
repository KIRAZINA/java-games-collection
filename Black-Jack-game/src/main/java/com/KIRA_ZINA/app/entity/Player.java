package com.KIRA_ZINA.app.entity;

import com.KIRA_ZINA.app.model.Hand;
import com.KIRA_ZINA.app.model.Card;

/**
 * Represents a player in the blackjack game
 */
public class Player {
    protected Hand hand;

    public Player() {
        this.hand = new Hand();
    }

    /**
     * Gets the player's hand
     */
    public Hand getHand() {
        return hand;
    }

    /**
     * Adds a card to the player's hand
     */
    public void addCardToHand(Card card) {
        hand.addCard(card);
    }

    /**
     * Gets the value of the player's hand
     */
    public int getHandValue() {
        return hand.getValue();
    }

    /**
     * Checks if the player has blackjack
     */
    public boolean hasBlackjack() {
        return hand.isBlackjack();
    }

    /**
     * Checks if the player has busted
     */
    public boolean isBust() {
        return hand.isBust();
    }

    /**
     * Clears the player's hand
     */
    public void clearHand() {
        hand.clear();
    }
}