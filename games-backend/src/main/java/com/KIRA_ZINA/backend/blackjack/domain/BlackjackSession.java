package com.KIRA_ZINA.backend.blackjack.domain;

import java.time.Instant;
import java.util.List;

public final class BlackjackSession {
    public static final double MIN_BET = 1.0;
    public static final double MAX_BET = 1000.0;

    private final String id;
    private final Hand playerHand = new Hand();
    private final Hand dealerHand = new Hand();
    private Deck deck = new Deck();
    private DealerDifficulty difficulty;
    private RoundPhase phase = RoundPhase.BETTING;
    private RoundWinner winner = RoundWinner.NONE;
    private double balance;
    private double currentBet;
    private Instant lastTouched = Instant.now();

    public BlackjackSession(String id, double initialBalance, DealerDifficulty difficulty) {
        if (initialBalance < MIN_BET) {
            throw new IllegalArgumentException("Initial balance must be at least " + MIN_BET);
        }
        this.id = id;
        this.balance = initialBalance;
        this.difficulty = difficulty;
    }

    public String getId() {
        return id;
    }

    public synchronized BlackjackState startRound() {
        ensureOpen();
        if (balance < MIN_BET) {
            throw new IllegalStateException("Insufficient balance to start a new round");
        }
        playerHand.clear();
        dealerHand.clear();
        currentBet = 0;
        winner = RoundWinner.NONE;
        phase = RoundPhase.BETTING;
        if (deck.remainingCards() < 12) {
            deck = new Deck();
        }
        deck.shuffle();
        touch();
        return snapshot(false);
    }

    public synchronized BlackjackState placeBet(double amount) {
        ensurePhase(RoundPhase.BETTING);
        validateBet(amount);
        balance -= amount;
        currentBet = amount;
        playerHand.add(deck.deal());
        playerHand.add(deck.deal());
        dealerHand.add(deck.deal());
        dealerHand.add(deck.deal());
        phase = RoundPhase.PLAYER_TURN;
        if (playerHand.blackjack() || dealerHand.blackjack()) {
            settleRound();
        }
        touch();
        return snapshot(false);
    }

    public synchronized BlackjackState hit() {
        ensurePhase(RoundPhase.PLAYER_TURN);
        playerHand.add(deck.deal());
        if (playerHand.bust()) {
            settleRound();
        }
        touch();
        return snapshot(phase == RoundPhase.ROUND_OVER);
    }

    public synchronized BlackjackState stand() {
        ensurePhase(RoundPhase.PLAYER_TURN);
        phase = RoundPhase.DEALER_TURN;
        while (difficulty.shouldHit(dealerHand.value(), deck.remainingCards())) {
            dealerHand.add(deck.deal());
            if (dealerHand.bust()) {
                break;
            }
        }
        settleRound();
        touch();
        return snapshot(true);
    }

    public synchronized BlackjackState state() {
        touch();
        return snapshot(phase == RoundPhase.ROUND_OVER || phase == RoundPhase.SESSION_CLOSED);
    }

    public synchronized void close() {
        playerHand.clear();
        dealerHand.clear();
        currentBet = 0;
        phase = RoundPhase.SESSION_CLOSED;
        touch();
    }

    public synchronized Instant lastTouched() {
        return lastTouched;
    }

    private void settleRound() {
        int playerValue = playerHand.value();
        int dealerValue = dealerHand.value();

        if (playerHand.blackjack() && !dealerHand.blackjack()) {
            winner = RoundWinner.PLAYER;
            balance += currentBet * 2.5;
        } else if (dealerHand.blackjack() && !playerHand.blackjack()) {
            winner = RoundWinner.DEALER;
        } else if (playerHand.blackjack() && dealerHand.blackjack()) {
            winner = RoundWinner.TIE;
            balance += currentBet;
        } else if (playerHand.bust()) {
            winner = RoundWinner.DEALER;
        } else if (dealerHand.bust() || playerValue > dealerValue) {
            winner = RoundWinner.PLAYER;
            balance += currentBet * 2;
        } else if (dealerValue > playerValue) {
            winner = RoundWinner.DEALER;
        } else {
            winner = RoundWinner.TIE;
            balance += currentBet;
        }

        currentBet = 0;
        phase = RoundPhase.ROUND_OVER;
    }

    private BlackjackState snapshot(boolean revealDealerHand) {
        List<Card> dealerCards = revealDealerHand || dealerHand.cards().isEmpty()
                ? dealerHand.cards()
                : List.of(dealerHand.cards().get(0));
        Integer dealerValue = revealDealerHand ? dealerHand.value() : null;
        return new BlackjackState(
                id,
                phase,
                winner,
                difficulty,
                balance,
                currentBet,
                playerHand.cards(),
                playerHand.value(),
                dealerCards,
                dealerValue,
                deck.remainingCards(),
                balance >= MIN_BET
        );
    }

    private void validateBet(double amount) {
        if (amount < MIN_BET) {
            throw new IllegalArgumentException("Minimum bet is " + MIN_BET);
        }
        if (amount > MAX_BET) {
            throw new IllegalArgumentException("Maximum bet is " + MAX_BET);
        }
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient balance");
        }
    }

    private void ensurePhase(RoundPhase expected) {
        ensureOpen();
        if (phase != expected) {
            throw new IllegalStateException("Expected phase " + expected + " but was " + phase);
        }
    }

    private void ensureOpen() {
        if (phase == RoundPhase.SESSION_CLOSED) {
            throw new IllegalStateException("Session is closed");
        }
    }

    private void touch() {
        lastTouched = Instant.now();
    }
}
