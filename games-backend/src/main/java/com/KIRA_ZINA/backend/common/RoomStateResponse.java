package com.KIRA_ZINA.backend.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.KIRA_ZINA.backend.blackjack.domain.Card;

import java.util.List;
import java.util.Map;

public record RoomStateResponse(
        String roomId,
        String gameType,
        String state,
        int playerCount,
        List<PlayerState> players,
        String roomPhase,
        long timeRemaining,
        long gameStartTime,
        boolean allPlayersReady,
        int readyCount,
        int totalPlayers,
        // Settled-result exposure (Step 6b.3): settleGame() computes both on
        // GameRoom but nothing observable carried them before this. Both stay
        // null until the room actually reaches GAME_OVER with a winner, so a
        // null winnerId means "not settled (or no score could be extracted)".
        String winnerId,
        Integer winnerScore
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PlayerState(
            String playerId,
            String playerName,
            Map<String, Object> metrics,
            // Step 6e: additive opponent-card projection. Absent (JSON null ->
            // omitted) for every non-blackjack room and for a blackjack player
            // whose session has not dealt cards yet (BETTING); the non-null
            // case carries that player's live hand and the dealer's face-up card.
            BlackjackView blackjack
    ) {
    }

    /**
     * Read-only projection of one player's blackjack hand onto the room state
     * payload (Step 6e, A1). Built from that player's own
     * {@code BlackjackSessionService.state(sessionId)} snapshot, so the dealer
     * hole-card reveal rule is the pre-existing one ({@code snapshot} only
     * exposes the full dealer hand at ROUND_OVER) - no new reveal logic is
     * introduced. While that player's round is live, {@code dealerCards} holds
     * only the face-up card and {@code dealerValue} is null; once the round
     * settles, the full dealer hand and its value are present. The card lists
     * are immutable copies (Hand.cards() returns List.copyOf), so the room
     * poll cannot observe a half-mutated hand.
     */
    public record BlackjackView(
            List<Card> playerCards,
            int playerValue,
            List<Card> dealerCards,
            Integer dealerValue,
            String phase
    ) {
    }
}
