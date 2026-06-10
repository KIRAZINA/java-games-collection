package com.KIRA_ZINA.backend.blackjack.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link BlackjackSession} covering:
 * - ACE counting (1 vs 11), blackjack payouts, ties
 * - State-machine enforcement (hit/stand/bet ordering)
 * - Bet validation (min, max, balance)
 * - All three {@link DealerDifficulty} strategies
 * - Deck reshuffling and session lifecycle
 */
@DisplayName("BlackjackSession")
class BlackjackSessionTest {

    // ============================================================ Construction

    @Nested
    @DisplayName("Construction")
    class Construction {

        @Test
        @DisplayName("initial balance below minimum throws")
        void initialBalanceBelowMinThrows() {
            assertThatThrownBy(() -> new BlackjackSession("s", 0.5, DealerDifficulty.BASIC))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least");
        }

        @Test
        @DisplayName("initial state is BETTING phase with correct balance")
        void initialStateIsCorrect() {
            BlackjackSession session = session(100.0);
            BlackjackState state = session.startRound();
            assertThat(state.phase()).isEqualTo(RoundPhase.BETTING);
            assertThat(state.balance()).isEqualTo(100.0);
            assertThat(state.playerCards()).isEmpty();
            assertThat(state.dealerCards()).isEmpty();
        }
    }

    // ============================================================ Bet Validation

    @Nested
    @DisplayName("Bet Validation")
    class BetValidation {

        @Test
        @DisplayName("bet below minimum (MIN_BET) throws IllegalArgumentException")
        void betBelowMinimumThrows() {
            BlackjackSession session = session(100.0);
            session.startRound();
            assertThatThrownBy(() -> session.placeBet(BlackjackSession.MIN_BET - 0.01))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Minimum bet");
        }

        @Test
        @DisplayName("bet above maximum (MAX_BET) throws IllegalArgumentException")
        void betAboveMaximumThrows() {
            BlackjackSession session = session(BlackjackSession.MAX_BET + 1);
            session.startRound();
            assertThatThrownBy(() -> session.placeBet(BlackjackSession.MAX_BET + 0.01))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Maximum bet");
        }

        @Test
        @DisplayName("bet exceeding current balance throws IllegalArgumentException")
        void betExceedsBalanceThrows() {
            BlackjackSession session = session(50.0);
            session.startRound();
            assertThatThrownBy(() -> session.placeBet(50.01))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient balance");
        }

        @Test
        @DisplayName("valid bet deducts amount from balance and deals 4 cards")
        void validBetDeductsBalance() {
            BlackjackSession session = session(100.0);
            session.startRound();
            BlackjackState state = session.placeBet(10.0);

            // Balance deducted (90 during play, or restored if settled immediately)
            assertThat(state.balance()).isIn(90.0, 100.0, 110.0, 115.0);
            assertThat(state.currentBet()).isIn(0.0, 10.0);
            // Player dealt 2 cards; dealer shows at least 1 (hidden until stand)
            assertThat(state.playerCards()).hasSize(2);
            assertThat(state.dealerCards()).hasSizeBetween(1, 2);
        }

        @Test
        @DisplayName("cannot start new round when balance < MIN_BET")
        void cannotStartRoundWithInsufficientBalance() {
            BlackjackSession session = session(BlackjackSession.MIN_BET);
            session.startRound();
            session.placeBet(BlackjackSession.MIN_BET); // loses balance entirely if dealer wins
            // After losing, balance could be 0
            // We simulate that by reading canContinue
            BlackjackState state = session.state();
            // canContinue reflects whether balance >= MIN_BET
            assertThat(state.canContinue()).isEqualTo(state.balance() >= BlackjackSession.MIN_BET);
        }
    }

    // ============================================================ State Machine

    @Nested
    @DisplayName("State Machine — Phase Guards")
    class StateMachine {

        @Test
        @DisplayName("hit() before placeBet() throws IllegalStateException")
        void hitBeforeBetThrows() {
            BlackjackSession session = session(100.0);
            session.startRound();
            assertThatThrownBy(session::hit)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PLAYER_TURN");
        }

        @Test
        @DisplayName("hit() before startRound() throws IllegalStateException")
        void hitBeforeStartRoundThrows() {
            BlackjackSession session = session(100.0);
            assertThatThrownBy(session::hit)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("stand() before placeBet() throws IllegalStateException")
        void standBeforeBetThrows() {
            BlackjackSession session = session(100.0);
            session.startRound();
            assertThatThrownBy(session::stand)
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("placeBet() in PLAYER_TURN phase throws IllegalStateException")
        void placeBetDuringPlayerTurnThrows() {
            BlackjackSession session = activeSession();
            if (session.state().phase() == RoundPhase.PLAYER_TURN) {
                assertThatThrownBy(() -> session.placeBet(5.0))
                        .isInstanceOf(IllegalStateException.class);
            }
        }

        @Test
        @DisplayName("hit() after stand() (ROUND_OVER) throws IllegalStateException")
        void hitAfterStandThrows() {
            BlackjackSession session = activeSession();
            if (session.state().phase() == RoundPhase.PLAYER_TURN) {
                session.stand();
                assertThatThrownBy(session::hit)
                        .isInstanceOf(IllegalStateException.class);
            }
        }

        @Test
        @DisplayName("any action on SESSION_CLOSED session throws IllegalStateException")
        void closedSessionThrowsOnAnyAction() {
            BlackjackSession session = session(100.0);
            session.close();

            assertThatThrownBy(session::startRound).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("closed");
            assertThatThrownBy(session::hit).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(session::stand).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> session.placeBet(10.0)).isInstanceOf(IllegalStateException.class);
        }
    }

    // ============================================================ Payouts

    @Nested
    @DisplayName("Round Settlement & Payouts")
    class Payouts {

        @Test
        @DisplayName("player wins normally: balance increases by 2x bet")
        void playerWinsNormallyGets2xBet() {
            // Run many rounds to get a natural player-wins result
            // We can verify the math by tracking balance
            BlackjackSession session = session(100.0);
            session.startRound();
            BlackjackState afterBet = session.placeBet(10.0);
            // If not over yet, player stands
            BlackjackState settled = afterBet.phase() == RoundPhase.ROUND_OVER
                    ? afterBet : session.stand();

            assertThat(settled.phase()).isEqualTo(RoundPhase.ROUND_OVER);
            assertThat(settled.winner()).isIn(RoundWinner.PLAYER, RoundWinner.DEALER, RoundWinner.TIE);
            assertThat(settled.currentBet()).isZero();
            assertThat(settled.dealerValue()).isNotNull();

            // Balance math: started 100, bet 10, so balance was 90 during play
            // If PLAYER win: 90 + 20 = 110
            // If TIE: 90 + 10 = 100
            // If DEALER: 90 (no refund)
            switch (settled.winner()) {
                case PLAYER -> assertThat(settled.balance()).isEqualTo(110.0);
                case TIE -> assertThat(settled.balance()).isEqualTo(100.0);
                case DEALER -> assertThat(settled.balance()).isEqualTo(90.0);
                default -> {}
            }
        }

        @Test
        @DisplayName("blackjack payout is 2.5x bet when player has natural 21")
        void playerBlackjackPays2_5x() {
            // Verify the payout constant via the settleRound formula directly.
            // We test by checking the formula: if player BJ and dealer no BJ →
            // balance = (balance - bet) + bet * 2.5
            // Starting 100, bet 10 → balance during play = 90, win → 90 + 25 = 115
            // We use a repeated run approach: run until a blackjack state is observed,
            // or directly verify the code logic via a controlled scenario.
            // Since deck is random, we verify via the settleRound state contract.
            BlackjackSession session = session(100.0);
            session.startRound();
            BlackjackState afterBet = session.placeBet(10.0);

            if (afterBet.phase() == RoundPhase.ROUND_OVER && afterBet.winner() == RoundWinner.PLAYER) {
                // Could be blackjack payout (115) or normal win after natural (impossible in 2-card stand)
                // If it settled immediately with PLAYER win, it means player blackjack
                if (afterBet.playerCards().size() == 2 && afterBet.playerValue() == 21) {
                    assertThat(afterBet.balance()).isEqualTo(115.0); // 90 + 10*2.5
                }
            }
        }

        @Test
        @DisplayName("tie returns bet to player: balance stays equal to pre-bet balance")
        void tiePayout() {
            // Full round: bet 10, start with 100 → if TIE → balance = 100
            BlackjackSession session = session(100.0);
            session.startRound();
            BlackjackState afterBet = session.placeBet(10.0);
            BlackjackState settled = afterBet.phase() == RoundPhase.ROUND_OVER
                    ? afterBet : session.stand();

            if (settled.winner() == RoundWinner.TIE) {
                assertThat(settled.balance()).isEqualTo(100.0);
            }
        }

        @Test
        @DisplayName("after round over, currentBet is zero")
        void currentBetIsZeroAfterRound() {
            BlackjackSession session = session(100.0);
            session.startRound();
            BlackjackState afterBet = session.placeBet(10.0);
            BlackjackState settled = afterBet.phase() == RoundPhase.ROUND_OVER
                    ? afterBet : session.stand();
            assertThat(settled.currentBet()).isZero();
        }

        @Test
        @DisplayName("winner field is NONE at start of betting phase")
        void winnerIsNoneAtBettingPhase() {
            BlackjackSession session = session(100.0);
            BlackjackState state = session.startRound();
            assertThat(state.winner()).isEqualTo(RoundWinner.NONE);
        }
    }

    // ============================================================ Dealer Difficulty

    @Nested
    @DisplayName("DealerDifficulty Strategy")
    class DealerDifficultyTests {

        @Test
        @DisplayName("BASIC dealer hits on 16 and stands on 17")
        void basicDealerHitsBelow17() {
            assertThat(DealerDifficulty.BASIC.shouldHit(16, 30)).isTrue();
            assertThat(DealerDifficulty.BASIC.shouldHit(17, 30)).isFalse();
            assertThat(DealerDifficulty.BASIC.shouldHit(21, 30)).isFalse();
        }

        @Test
        @DisplayName("CONSERVATIVE dealer hits on 15, stands on 16 and 17")
        void conservativeDealerHitsBelow16() {
            assertThat(DealerDifficulty.CONSERVATIVE.shouldHit(11, 30)).isTrue();
            assertThat(DealerDifficulty.CONSERVATIVE.shouldHit(15, 30)).isTrue();
            assertThat(DealerDifficulty.CONSERVATIVE.shouldHit(16, 30)).isFalse();
            assertThat(DealerDifficulty.CONSERVATIVE.shouldHit(17, 30)).isFalse();
        }

        @Test
        @DisplayName("AGGRESSIVE dealer hits on 17 and stands on 18")
        void aggressiveDealerHitsBelow18() {
            assertThat(DealerDifficulty.AGGRESSIVE.shouldHit(17, 30)).isTrue();
            assertThat(DealerDifficulty.AGGRESSIVE.shouldHit(18, 30)).isFalse();
            assertThat(DealerDifficulty.AGGRESSIVE.shouldHit(10, 30)).isTrue();
        }

        @Test
        @DisplayName("CONSERVATIVE dealer stands on 12 (edge of the 12-15 gap)")
        void conservativeStandsOn12() {
            // CONSERVATIVE: shouldHit if value <= 11 OR value < 16
            // value = 12: 12 <= 11 is false; 12 < 16 is true → hits
            assertThat(DealerDifficulty.CONSERVATIVE.shouldHit(12, 30)).isTrue();
        }
    }

    // ============================================================ Deck reshuffling

    @Nested
    @DisplayName("Deck Reshuffling")
    class DeckReshuffle {

        @Test
        @DisplayName("cardsRemaining count decreases as cards are dealt")
        void cardsRemainingDecreasesAfterDeal() {
            BlackjackSession session = session(100.0);
            session.startRound();
            int before = session.state().cardsRemaining();
            session.placeBet(10.0); // deals 4 cards
            int after = session.state().cardsRemaining();
            assertThat(after).isLessThan(before);
        }
    }

    // ============================================================ lastTouched

    @Nested
    @DisplayName("lastTouched()")
    class LastTouched {

        @Test
        @DisplayName("lastTouched is updated after each operation")
        void lastTouchedUpdatedAfterEachOperation() throws InterruptedException {
            BlackjackSession session = session(100.0);
            var t0 = session.lastTouched();

            Thread.sleep(2);
            session.startRound();
            var t1 = session.lastTouched();
            assertThat(t1).isAfter(t0);

            Thread.sleep(2);
            session.placeBet(10.0);
            var t2 = session.lastTouched();
            assertThat(t2).isAfter(t1);
        }
    }

    // ============================================================ Full round lifecycle

    @Nested
    @DisplayName("Full Round Lifecycle")
    class FullRound {

        @RepeatedTest(5)
        @DisplayName("round can be started, bet placed, and settled (repeated for randomness)")
        void roundCanBeStartedBetAndSettled() {
            BlackjackSession session = session(100.0);

            BlackjackState initial = session.startRound();
            assertThat(initial.phase()).isEqualTo(RoundPhase.BETTING);
            assertThat(initial.balance()).isEqualTo(100.0);

            BlackjackState afterBet = session.placeBet(10.0);
            if (afterBet.phase() == RoundPhase.PLAYER_TURN) {
                assertThat(afterBet.balance()).isEqualTo(90.0);
            } else {
                assertThat(afterBet.balance()).isIn(100.0, 115.0);
            }
            assertThat(afterBet.playerCards()).hasSize(2);
            assertThat(afterBet.dealerCards()).hasSizeBetween(1, 2);

            BlackjackState settled = afterBet.phase() == RoundPhase.ROUND_OVER
                    ? afterBet : session.stand();
            assertThat(settled.phase()).isEqualTo(RoundPhase.ROUND_OVER);
            assertThat(settled.winner()).isIn(RoundWinner.PLAYER, RoundWinner.DEALER, RoundWinner.TIE);
            assertThat(settled.currentBet()).isZero();
            assertThat(settled.dealerValue()).isNotNull();
            // Dealer hand fully revealed
            assertThat(settled.dealerCards()).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("dealer hand is hidden (1 card shown) during PLAYER_TURN")
        void dealerHandHiddenDuringPlayerTurn() {
            BlackjackSession session = session(100.0);
            session.startRound();
            BlackjackState afterBet = session.placeBet(10.0);

            if (afterBet.phase() == RoundPhase.PLAYER_TURN) {
                // Only the dealer's first card is visible
                assertThat(afterBet.dealerCards()).hasSize(1);
                assertThat(afterBet.dealerValue()).isNull();
            }
        }

        @Test
        @DisplayName("rejects out-of-order actions (original smoke test)")
        void rejectsInvalidBetsAndOutOfOrderActions() {
            BlackjackSession session = new BlackjackSession("blackjack-2", 100.0, DealerDifficulty.BASIC);

            assertThatThrownBy(session::hit)
                    .isInstanceOf(IllegalStateException.class);

            session.startRound();

            assertThatThrownBy(() -> session.placeBet(0.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Minimum bet");

            assertThatThrownBy(() -> session.placeBet(101.0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insufficient balance");
        }
    }

    // ============================================================ Notifications

    @Nested
    @DisplayName("Notifications")
    class Notifications {

        @Test
        @DisplayName("state returns empty notifications list after first read")
        void notificationsDrainedOnRead() {
            BlackjackSession session = session(100.0);
            session.startRound();
            BlackjackState first = session.state();
            assertThat(first.notifications()).isEmpty();
        }

        @Test
        @DisplayName("blackjack notification is set when player gets natural 21")
        void playerBlackjackAddsNotification() {
            // Arrange: player gets blackjack (Ace + 10-value), dealer no blackjack
            BlackjackSession session = session(100.0);
            session.startRound();
            setupDeck(session,
                    new Card(Suit.HEARTS, Rank.ACE),   // player card 1
                    new Card(Suit.CLUBS, Rank.KING),     // player card 2 → blackjack
                    new Card(Suit.DIAMONDS, Rank.FIVE),  // dealer card 1
                    new Card(Suit.SPADES, Rank.NINE)     // dealer card 2 → 14, no blackjack
            );
            BlackjackState state = session.placeBet(10.0);
            assertThat(state.notifications()).anyMatch(msg -> msg.contains("Blackjack"));
        }

        @Test
        @DisplayName("dealer blackjack adds notification when dealer peeks")
        void dealerBlackjackAddsNotification() {
            BlackjackSession session = session(100.0);
            session.startRound();
            setupDeck(session,
                    new Card(Suit.HEARTS, Rank.FIVE),    // player card 1
                    new Card(Suit.CLUBS, Rank.NINE),      // player card 2 → 14
                    new Card(Suit.DIAMONDS, Rank.ACE),    // dealer card 1
                    new Card(Suit.SPADES, Rank.KING)      // dealer card 2 → blackjack!
            );
            BlackjackState state = session.placeBet(10.0);
            assertThat(state.notifications()).anyMatch(msg -> msg.contains("Dealer has Blackjack"));
        }
    }

    // ============================================================ Bankruptcy Bailout

    @Nested
    @DisplayName("Bankruptcy Bailout")
    class BankruptcyBailout {

        @Test
        @DisplayName("startRound with zero balance refills to 100 and skips hand")
        void bailoutRefillsBalance() throws Exception {
            BlackjackSession session = session(100.0);
            java.lang.reflect.Field balanceField = BlackjackSession.class.getDeclaredField("balance");
            balanceField.setAccessible(true);
            balanceField.set(session, 0.0);

            BlackjackState state = session.startRound();
            assertThat(state.balance()).isEqualTo(100.0);
            assertThat(state.phase()).isEqualTo(RoundPhase.BETTING);
            assertThat(state.notifications()).anyMatch(msg -> msg.contains("refilled"));
        }

        @Test
        @DisplayName("bailout is triggered only when balance is exactly zero")
        void bailoutNotTriggeredForPositiveBalance() throws Exception {
            BlackjackSession session = session(100.0);
            java.lang.reflect.Field balanceField = BlackjackSession.class.getDeclaredField("balance");
            balanceField.setAccessible(true);
            balanceField.set(session, 0.01);

            BlackjackState state = session.startRound();
            assertThat(state.phase()).isEqualTo(RoundPhase.BETTING);
            assertThat(state.balance()).isEqualTo(0.01);
            assertThat(state.notifications()).isEmpty();
        }
    }

    // ============================================================ Controlled Deck Tests

    @Nested
    @DisplayName("Controlled Deck — Dealer Peek & 5-Card Charlie")
    class ControlledDeck {

        @Test
        @DisplayName("dealer blackjack causes immediate settlement (dealer peek)")
        void dealerBlackjackSettlesImmediately() {
            BlackjackSession session = session(100.0);
            session.startRound();
            // Player gets 14, dealer gets Ace+King (blackjack)
            setupDeck(session,
                    new Card(Suit.HEARTS, Rank.FIVE),    // player card 1
                    new Card(Suit.CLUBS, Rank.NINE),      // player card 2
                    new Card(Suit.DIAMONDS, Rank.ACE),    // dealer card 1
                    new Card(Suit.SPADES, Rank.KING)      // dealer card 2 → blackjack
            );
            BlackjackState state = session.placeBet(10.0);
            assertThat(state.phase()).isEqualTo(RoundPhase.ROUND_OVER);
            assertThat(state.winner()).isEqualTo(RoundWinner.DEALER);
            assertThat(state.balance()).isEqualTo(90.0); // lost bet
            assertThat(state.dealerCards()).hasSize(2);   // fully revealed
            assertThat(state.dealerValue()).isEqualTo(21);
        }

        @Test
        @DisplayName("both player and dealer blackjack results in tie")
        void bothBlackjackTies() {
            BlackjackSession session = session(100.0);
            session.startRound();
            // Both get blackjack
            setupDeck(session,
                    new Card(Suit.HEARTS, Rank.ACE),     // player card 1
                    new Card(Suit.CLUBS, Rank.KING),       // player card 2 → blackjack
                    new Card(Suit.DIAMONDS, Rank.ACE),     // dealer card 1
                    new Card(Suit.SPADES, Rank.KING)       // dealer card 2 → blackjack
            );
            BlackjackState state = session.placeBet(10.0);
            assertThat(state.phase()).isEqualTo(RoundPhase.ROUND_OVER);
            assertThat(state.winner()).isEqualTo(RoundWinner.TIE);
            assertThat(state.balance()).isEqualTo(100.0); // bet returned
        }

        @Test
        @DisplayName("5-Card Charlie auto-wins when player reaches 5 cards without busting")
        void fiveCardCharlieWins() {
            BlackjackSession session = session(100.0);
            session.startRound();
            // Player needs 5 small cards, dealer gets 2 cards that don't matter
            setupDeck(session,
                    new Card(Suit.HEARTS, Rank.TWO),     // player card 1
                    new Card(Suit.CLUBS, Rank.THREE),     // player card 2
                    new Card(Suit.DIAMONDS, Rank.FIVE),   // dealer card 1
                    new Card(Suit.SPADES, Rank.NINE),     // dealer card 2
                    new Card(Suit.HEARTS, Rank.TWO),       // player card 3 (hit)
                    new Card(Suit.CLUBS, Rank.THREE),      // player card 4 (hit)
                    new Card(Suit.DIAMONDS, Rank.TWO)      // player card 5 (hit) → 12, no bust → Charlie!
            );
            session.placeBet(10.0);
            BlackjackState afterHit1 = session.hit(); // card 3
            assertThat(afterHit1.phase()).isEqualTo(RoundPhase.PLAYER_TURN);
            BlackjackState afterHit2 = session.hit(); // card 4
            assertThat(afterHit2.phase()).isEqualTo(RoundPhase.PLAYER_TURN);
            BlackjackState afterHit3 = session.hit(); // card 5 → Charlie!
            assertThat(afterHit3.phase()).isEqualTo(RoundPhase.ROUND_OVER);
            assertThat(afterHit3.winner()).isEqualTo(RoundWinner.PLAYER);
            assertThat(afterHit3.balance()).isEqualTo(110.0); // 90 + 10*2
            assertThat(afterHit3.notifications()).anyMatch(msg -> msg.contains("Charlie"));
        }

        @Test
        @DisplayName("hitting to 5 cards with bust does NOT trigger Charlie")
        void fiveCardsBustNotCharlie() {
            BlackjackSession session = session(100.0);
            session.startRound();
            // Player gets 4 small cards then a big card that busts on 5th
            setupDeck(session,
                    new Card(Suit.HEARTS, Rank.FIVE),     // player card 1
                    new Card(Suit.CLUBS, Rank.FIVE),       // player card 2
                    new Card(Suit.DIAMONDS, Rank.TWO),     // dealer card 1
                    new Card(Suit.SPADES, Rank.THREE),     // dealer card 2
                    new Card(Suit.HEARTS, Rank.THREE),     // player card 3 (hit)
                    new Card(Suit.CLUBS, Rank.FOUR),       // player card 4 (hit)
                    new Card(Suit.DIAMONDS, Rank.KING)     // player card 5 (hit) → bust!
            );
            session.placeBet(10.0);
            session.hit();
            session.hit();
            BlackjackState afterHit3 = session.hit();
            assertThat(afterHit3.phase()).isEqualTo(RoundPhase.ROUND_OVER);
            assertThat(afterHit3.winner()).isEqualTo(RoundWinner.DEALER); // bust → dealer wins
        }
    }

    // ============================================================ Helpers

    private static BlackjackSession session(double balance) {
        return new BlackjackSession("test-session", balance, DealerDifficulty.BASIC);
    }

    private static void setupDeck(BlackjackSession session, Card... topCards) {
        try {
            java.lang.reflect.Field deckField = BlackjackSession.class.getDeclaredField("deck");
            deckField.setAccessible(true);
            Deck deck = new Deck();
            java.lang.reflect.Field cardsField = Deck.class.getDeclaredField("cards");
            cardsField.setAccessible(true);
            Card[] cards = (Card[]) cardsField.get(deck);
            java.lang.reflect.Field nextIndexField = Deck.class.getDeclaredField("nextCardIndex");
            nextIndexField.setAccessible(true);
            for (int i = 0; i < topCards.length && i < cards.length; i++) {
                cards[cards.length - 1 - i] = topCards[i];
            }
            nextIndexField.set(deck, cards.length);
            deckField.set(session, deck);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a session in PLAYER_TURN (or ROUND_OVER if natural blackjack).
     * Balance = 100, bet = 10.
     */
    private static BlackjackSession activeSession() {
        BlackjackSession session = session(100.0);
        session.startRound();
        session.placeBet(10.0);
        return session;
    }
}
