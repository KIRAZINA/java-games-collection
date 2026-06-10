package com.KIRA_ZINA.backend.blackjack.domain;

import java.util.List;

public record BlackjackState(
        String sessionId,
        RoundPhase phase,
        RoundWinner winner,
        DealerDifficulty difficulty,
        double balance,
        double currentBet,
        List<Card> playerCards,
        int playerValue,
        List<Card> dealerCards,
        Integer dealerValue,
        int cardsRemaining,
        boolean canContinue,
        List<String> notifications
) {
}
