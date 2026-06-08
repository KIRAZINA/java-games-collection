package com.KIRA_ZINA.backend.blackjack.domain;

public record Card(Suit suit, Rank rank) {

    public int value() {
        return rank.value();
    }
}
