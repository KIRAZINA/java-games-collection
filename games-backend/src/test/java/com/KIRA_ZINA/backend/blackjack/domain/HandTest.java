package com.KIRA_ZINA.backend.blackjack.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Hand} — focuses on ACE counting (1 vs 11),
 * blackjack detection, bust detection, and clear().
 */
@DisplayName("Hand")
class HandTest {

    private Hand hand;

    @BeforeEach
    void setUp() {
        hand = new Hand();
    }

    // ------------------------------------------------------------------ value()

    @Nested
    @DisplayName("value()")
    class Value {

        @Test
        @DisplayName("empty hand has value 0")
        void emptyHandValueIsZero() {
            assertThat(hand.value()).isZero();
        }

        @Test
        @DisplayName("non-ace cards are summed directly")
        void nonAceCardsSum() {
            hand.add(card(Rank.KING));   // 10
            hand.add(card(Rank.QUEEN));  // 10
            assertThat(hand.value()).isEqualTo(20);
        }

        @Test
        @DisplayName("single ACE defaults to 11 when safe")
        void aceDefaultsTo11() {
            hand.add(card(Rank.ACE));
            assertThat(hand.value()).isEqualTo(11);
        }

        @Test
        @DisplayName("ACE + SEVEN = 18 (ACE counted as 11)")
        void aceCountedAs11WhenSafe() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.SEVEN));
            assertThat(hand.value()).isEqualTo(18);
        }

        @Test
        @DisplayName("ACE + NINE + FIVE = 15 (ACE downgraded to 1 to avoid bust)")
        void aceDowngradedTo1WhenBustWith11() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.NINE));
            hand.add(card(Rank.FIVE));
            // 11 + 9 + 5 = 25 → bust; downgrade ACE to 1 → 1 + 9 + 5 = 15
            assertThat(hand.value()).isEqualTo(15);
        }

        @Test
        @DisplayName("ACE + KING + ACE = 12 (second ACE forced to 1)")
        void twoAcesWithKing() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.KING));  // 10
            hand.add(card(Rank.ACE));
            // 11 + 10 = 21 → second ACE must be 1 → 11 + 10 + 1 = 22 bust → first ACE also 1 → 1 + 10 + 1 = 12
            assertThat(hand.value()).isEqualTo(12);
        }

        @Test
        @DisplayName("ACE + ACE = 12 (first as 11, second forced to 1)")
        void twoAcesEquals12() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.ACE));
            // 11 + 11 = 22 bust → second ACE = 1 → 11 + 1 = 12
            assertThat(hand.value()).isEqualTo(12);
        }

        @Test
        @DisplayName("ACE + ACE + NINE = 21")
        void twoAcesAndNineEquals21() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.NINE));
            // 11 + 1 + 9 = 21
            assertThat(hand.value()).isEqualTo(21);
        }

        @Test
        @DisplayName("ACE + ACE + NINE + TWO = 13 (both ACEs are 1)")
        void twoAcesAndNineAndTwoEquals13() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.NINE));
            hand.add(card(Rank.TWO));
            // 11 + 1 + 9 + 2 = 23 → bust, downgrade → 1 + 1 + 9 + 2 = 13
            assertThat(hand.value()).isEqualTo(13);
        }

        @Test
        @DisplayName("four ACEs = 14 (one as 11, three as 1)")
        void fourAces() {
            for (int i = 0; i < 4; i++) {
                hand.add(card(Rank.ACE));
            }
            assertThat(hand.value()).isEqualTo(14);
        }
    }

    // ---------------------------------------------------------------- blackjack()

    @Nested
    @DisplayName("blackjack()")
    class Blackjack {

        @Test
        @DisplayName("ACE + KING is blackjack")
        void aceAndKingIsBlackjack() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.KING));
            assertThat(hand.blackjack()).isTrue();
        }

        @Test
        @DisplayName("ACE + TEN is blackjack")
        void aceAndTenIsBlackjack() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.TEN));
            assertThat(hand.blackjack()).isTrue();
        }

        @Test
        @DisplayName("three cards summing to 21 is NOT blackjack")
        void threeCardsTo21IsNotBlackjack() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.FIVE));
            hand.add(card(Rank.FIVE));
            assertThat(hand.value()).isEqualTo(21);
            assertThat(hand.blackjack()).isFalse();
        }

        @Test
        @DisplayName("empty hand is not blackjack")
        void emptyHandIsNotBlackjack() {
            assertThat(hand.blackjack()).isFalse();
        }

        @Test
        @DisplayName("two cards totalling 20 is not blackjack")
        void twentyIsNotBlackjack() {
            hand.add(card(Rank.KING));
            hand.add(card(Rank.QUEEN));
            assertThat(hand.blackjack()).isFalse();
        }
    }

    // ------------------------------------------------------------------- bust()

    @Nested
    @DisplayName("bust()")
    class Bust {

        @Test
        @DisplayName("KING + QUEEN + TWO = 22 is bust")
        void twentyTwoIsBust() {
            hand.add(card(Rank.KING));
            hand.add(card(Rank.QUEEN));
            hand.add(card(Rank.TWO));
            assertThat(hand.bust()).isTrue();
        }

        @Test
        @DisplayName("21 is not a bust")
        void twentyOneIsNotBust() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.KING));
            assertThat(hand.bust()).isFalse();
        }

        @Test
        @DisplayName("ACE prevents bust when card would push over 21")
        void acePreventsBust() {
            hand.add(card(Rank.ACE));
            hand.add(card(Rank.KING));
            hand.add(card(Rank.FIVE));
            // 11 + 10 + 5 = 26 → ACE downgraded to 1 → 16
            assertThat(hand.bust()).isFalse();
            assertThat(hand.value()).isEqualTo(16);
        }
    }

    // ------------------------------------------------------------------ clear()

    @Nested
    @DisplayName("clear()")
    class Clear {

        @Test
        @DisplayName("clear resets value to 0 and removes all cards")
        void clearResetsHand() {
            hand.add(card(Rank.KING));
            hand.add(card(Rank.ACE));
            hand.clear();
            assertThat(hand.value()).isZero();
            assertThat(hand.cards()).isEmpty();
            assertThat(hand.blackjack()).isFalse();
            assertThat(hand.bust()).isFalse();
        }
    }

    // ------------------------------------------------------------------ helpers

    private static Card card(Rank rank) {
        return new Card(Suit.SPADES, rank);
    }
}
