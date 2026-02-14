package com.KIRA_ZINA.app.core;

import com.KIRA_ZINA.app.model.Card;
import com.KIRA_ZINA.app.strategy.BasicDealerStrategy;
import com.KIRA_ZINA.app.strategy.ConservativeDealerStrategy;
import com.KIRA_ZINA.app.exception.GameException;
import com.KIRA_ZINA.app.exception.InvalidBetException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Comprehensive unit tests for GameModel class
 */
public class GameModelTest {
    
    private GameModel model;
    private static final double INITIAL_BALANCE = 100.0;
    
    @Before
    public void setUp() {
        model = new GameModel(INITIAL_BALANCE);
        model.setDealerStrategy(new BasicDealerStrategy());
    }
    
    @Test
    public void testInitialGameState() {
        assertEquals(INITIAL_BALANCE, model.getBankroll().getBalance(), 0.001);
        assertEquals(0.0, model.getBankroll().getCurrentBet(), 0.001);
        assertFalse(model.isGameOver());
        assertNull(model.getWinner());
        assertTrue(model.isBettingPhase());
        assertNotNull(model.getDeck());
        assertNotNull(model.getPlayer());
        assertNotNull(model.getDealer());
    }
    
    @Test
    public void testInitializeRound() throws GameException {
        model.initializeRound();
        
        // Check that hands are cleared
        assertEquals(0, model.getPlayer().getHand().getCards().size());
        assertEquals(0, model.getDealer().getHand().getCards().size());
        
        // Check game state
        assertFalse(model.isGameOver());
        assertNull(model.getWinner());
        assertTrue(model.isBettingPhase());
    }
    
    @Test
    public void testPlaceBet() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);
        
        assertEquals(90.0, model.getBankroll().getBalance(), 0.001);
        assertEquals(10.0, model.getBankroll().getCurrentBet(), 0.001);
        assertFalse(model.isBettingPhase());
        
        // Check that cards were dealt
        assertEquals(2, model.getPlayer().getHand().getCards().size());
        assertEquals(2, model.getDealer().getHand().getCards().size());
    }
    
    @Test(expected = InvalidBetException.class)
    public void testPlaceInvalidBet() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(200.0); // More than balance
    }
    
    @Test
    public void testPlayerHit() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);
        
        int initialSize = model.getPlayer().getHand().getCards().size();
        model.playerHit();
        
        assertEquals(initialSize + 1, model.getPlayer().getHand().getCards().size());
    }
    
    @Test
    public void testDealerHit() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);
        
        int initialSize = model.getDealer().getHand().getCards().size();
        model.dealerHit();
        
        assertEquals(initialSize + 1, model.getDealer().getHand().getCards().size());
    }
    
    @Test
    public void testShouldDealerHit() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);

        // Ensure deterministic hand value: dealer has 13.
        model.getDealer().clearHand();
        model.getDealer().addCardToHand(new Card(Card.Suit.HEARTS, Card.Rank.EIGHT));
        model.getDealer().addCardToHand(new Card(Card.Suit.SPADES, Card.Rank.FIVE));

        assertTrue(model.shouldDealerHit());
    }
    
    @Test(expected = IllegalStateException.class)
    public void testShouldDealerHitWithoutStrategy() {
        GameModel modelWithoutStrategy = new GameModel(INITIAL_BALANCE);
        modelWithoutStrategy.shouldDealerHit();
    }
    
    @Test
    public void testDetermineWinnerPlayerWins() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);

        setHands(
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.TEN),
                        new Card(Card.Suit.HEARTS, Card.Rank.JACK)
                },
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.NINE),
                        new Card(Card.Suit.SPADES, Card.Rank.NINE)
                }
        );
        
        model.determineWinner();
        
        assertEquals("player", model.getWinner());
        assertTrue(model.isGameOver());
        assertEquals(110.0, model.getBankroll().getBalance(), 0.001);
    }
    
    @Test
    public void testDetermineWinnerDealerWins() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);

        setHands(
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.NINE),
                        new Card(Card.Suit.CLUBS, Card.Rank.NINE)
                },
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.TEN),
                        new Card(Card.Suit.DIAMONDS, Card.Rank.KING)
                }
        );
        
        model.determineWinner();
        
        assertEquals("dealer", model.getWinner());
        assertTrue(model.isGameOver());
        assertEquals(90.0, model.getBankroll().getBalance(), 0.001);
    }
    
    @Test
    public void testDetermineWinnerTie() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);

        setHands(
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.NINE),
                        new Card(Card.Suit.DIAMONDS, Card.Rank.NINE)
                },
                new Card[] {
                        new Card(Card.Suit.SPADES, Card.Rank.NINE),
                        new Card(Card.Suit.CLUBS, Card.Rank.NINE)
                }
        );
        
        model.determineWinner();
        
        assertEquals("tie", model.getWinner());
        assertTrue(model.isGameOver());
        assertEquals(100.0, model.getBankroll().getBalance(), 0.001);
    }
    
    @Test
    public void testDetermineWinnerPlayerBust() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);

        setHands(
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.TEN),
                        new Card(Card.Suit.SPADES, Card.Rank.JACK),
                        new Card(Card.Suit.DIAMONDS, Card.Rank.QUEEN)
                },
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.TWO),
                        new Card(Card.Suit.CLUBS, Card.Rank.THREE)
                }
        );
        
        model.determineWinner();
        
        assertEquals("dealer", model.getWinner());
        assertTrue(model.isGameOver());
        assertEquals(90.0, model.getBankroll().getBalance(), 0.001);
    }
    
    @Test
    public void testDetermineWinnerDealerBust() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);

        setHands(
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.TWO),
                        new Card(Card.Suit.SPADES, Card.Rank.THREE)
                },
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.TEN),
                        new Card(Card.Suit.SPADES, Card.Rank.JACK),
                        new Card(Card.Suit.DIAMONDS, Card.Rank.QUEEN)
                }
        );
        
        model.determineWinner();
        
        assertEquals("player", model.getWinner());
        assertTrue(model.isGameOver());
        assertEquals(110.0, model.getBankroll().getBalance(), 0.001);
    }
    
    @Test
    public void testDetermineWinnerPlayerBlackjack() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);

        setHands(
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.ACE),
                        new Card(Card.Suit.SPADES, Card.Rank.JACK)
                },
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.TEN),
                        new Card(Card.Suit.SPADES, Card.Rank.NINE)
                }
        );
        
        model.determineWinner();
        
        assertEquals("player", model.getWinner());
        assertTrue(model.isGameOver());
        assertEquals(115.0, model.getBankroll().getBalance(), 0.001); // Blackjack pays 2.5:1
    }
    
    @Test
    public void testDetermineWinnerDealerBlackjack() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);

        setHands(
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.TEN),
                        new Card(Card.Suit.SPADES, Card.Rank.NINE)
                },
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.ACE),
                        new Card(Card.Suit.SPADES, Card.Rank.JACK)
                }
        );
        
        model.determineWinner();
        
        assertEquals("dealer", model.getWinner());
        assertTrue(model.isGameOver());
        assertEquals(90.0, model.getBankroll().getBalance(), 0.001);
    }
    
    @Test
    public void testDetermineWinnerBothBlackjack() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);

        setHands(
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.ACE),
                        new Card(Card.Suit.SPADES, Card.Rank.JACK)
                },
                new Card[] {
                        new Card(Card.Suit.DIAMONDS, Card.Rank.ACE),
                        new Card(Card.Suit.CLUBS, Card.Rank.KING)
                }
        );
        
        model.determineWinner();
        
        assertEquals("tie", model.getWinner());
        assertTrue(model.isGameOver());
        assertEquals(100.0, model.getBankroll().getBalance(), 0.001);
    }
    
    @Test
    public void testCanContinuePlaying() throws InvalidBetException, GameException {
        assertTrue(model.canContinuePlaying());

        model.initializeRound();
        model.placeBet(100.0);
        setHands(
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.TEN),
                        new Card(Card.Suit.SPADES, Card.Rank.JACK),
                        new Card(Card.Suit.DIAMONDS, Card.Rank.QUEEN)
                },
                new Card[] {
                        new Card(Card.Suit.HEARTS, Card.Rank.TWO),
                        new Card(Card.Suit.CLUBS, Card.Rank.THREE)
                }
        );
        model.determineWinner();
        assertFalse(model.canContinuePlaying());
    }
    
    @Test
    public void testSetDealerStrategy() {
        ConservativeDealerStrategy conservative = new ConservativeDealerStrategy();
        model.setDealerStrategy(conservative);
        assertEquals(conservative, model.getDealerStrategy());
    }
    
    @Test
    public void testGetters() {
        assertNotNull(model.getDeck());
        assertNotNull(model.getPlayer());
        assertNotNull(model.getDealer());
        assertNotNull(model.getBankroll());
    }
    
    @Test(expected = GameException.class)
    public void testPlaceBetWithoutInitialization() throws GameException, InvalidBetException {
        // Try to place bet without initializing round
        model.placeBet(10.0);
    }
    
    @Test
    public void testMultipleRounds() throws GameException, InvalidBetException {
        // Play first round
        model.initializeRound();
        model.placeBet(10.0);
        model.determineWinner();
        
        // Reset for second round
        model.initializeRound();
        model.placeBet(5.0);
        model.determineWinner();

        assertNotNull(model.getWinner());
        assertTrue(model.isGameOver());
        assertEquals(0.0, model.getBankroll().getCurrentBet(), 0.001);
    }

    @Test(expected = IllegalStateException.class)
    public void testPlaceBetTwiceInSameRoundThrows() throws GameException, InvalidBetException {
        model.initializeRound();
        model.placeBet(10.0);
        model.placeBet(5.0);
    }

    @Test
    public void testInitializeRoundRecreatesDeckWhenExhausted() throws GameException, InvalidBetException {
        model.initializeRound();
        while (model.getDeck().size() > 0) {
            model.getDeck().dealCard();
        }
        assertEquals(0, model.getDeck().size());

        model.initializeRound();
        model.placeBet(10.0);

        // After initial deal: 52 - 4 cards on table = 48 cards left.
        assertEquals(48, model.getDeck().size());
    }

    private void setHands(Card[] playerCards, Card[] dealerCards) {
        model.getPlayer().clearHand();
        model.getDealer().clearHand();

        for (Card card : playerCards) {
            model.getPlayer().addCardToHand(card);
        }
        for (Card card : dealerCards) {
            model.getDealer().addCardToHand(card);
        }
    }
}
