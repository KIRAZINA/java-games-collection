package com.KIRA_ZINA.backend.blackjack.domain;

import java.util.concurrent.ThreadLocalRandom;

public final class Deck {
    private static final Card[] STANDARD_DECK = createStandardDeck();

    private final Card[] cards;
    private int nextCardIndex;

    public Deck() {
        this.cards = STANDARD_DECK.clone();
        this.nextCardIndex = cards.length;
    }

    public void shuffle() {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = nextCardIndex - 1; i > 0; i--) {
            int selected = random.nextInt(i + 1);
            Card current = cards[i];
            cards[i] = cards[selected];
            cards[selected] = current;
        }
    }

    public Card deal() {
        if (nextCardIndex == 0) {
            throw new IllegalStateException("Cannot deal from an empty deck");
        }
        return cards[--nextCardIndex];
    }

    public int remainingCards() {
        return nextCardIndex;
    }

    private static Card[] createStandardDeck() {
        Card[] deck = new Card[Suit.values().length * Rank.values().length];
        int index = 0;
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                deck[index++] = new Card(suit, rank);
            }
        }
        return deck;
    }
}
