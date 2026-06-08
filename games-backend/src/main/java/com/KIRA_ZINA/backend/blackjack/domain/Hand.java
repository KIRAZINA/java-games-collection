package com.KIRA_ZINA.backend.blackjack.domain;

import java.util.ArrayList;
import java.util.List;

public final class Hand {
    private final List<Card> cards = new ArrayList<>(8);

    public void add(Card card) {
        cards.add(card);
    }

    public int value() {
        int value = 0;
        int aces = 0;
        for (Card card : cards) {
            if (card.rank() == Rank.ACE) {
                aces++;
            } else {
                value += card.value();
            }
        }
        // Count all aces as 1 first
        value += aces;
        // If we have at least one ace and upgrading one of them to 11 (adding 10) does not bust, do it
        if (aces > 0 && value + 10 <= 21) {
            value += 10;
        }
        return value;
    }

    public boolean blackjack() {
        return cards.size() == 2 && value() == 21;
    }

    public boolean bust() {
        return value() > 21;
    }

    public List<Card> cards() {
        return List.copyOf(cards);
    }

    public void clear() {
        cards.clear();
    }
}
