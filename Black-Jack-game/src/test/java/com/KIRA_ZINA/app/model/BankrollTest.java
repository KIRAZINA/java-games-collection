package com.KIRA_ZINA.app.model;

import com.KIRA_ZINA.app.exception.InvalidBetException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for Bankroll class
 */
public class BankrollTest {
    
    private Bankroll bankroll;
    
    @Before
    public void setUp() {
        bankroll = new Bankroll(100.0);
    }
    
    @Test
    public void testInitialBalance() {
        assertEquals(100.0, bankroll.getBalance(), 0.001);
        assertEquals(0.0, bankroll.getCurrentBet(), 0.001);
    }
    
    @Test
    public void testValidBet() throws InvalidBetException {
        bankroll.placeBet(10.0);
        assertEquals(90.0, bankroll.getBalance(), 0.001);
        assertEquals(10.0, bankroll.getCurrentBet(), 0.001);
    }
    
    @Test(expected = InvalidBetException.class)
    public void testBetExceedsBalance() throws InvalidBetException {
        bankroll.placeBet(150.0);
    }
    
    @Test(expected = InvalidBetException.class)
    public void testBetBelowMinimum() throws InvalidBetException {
        bankroll.placeBet(0.5);
    }
    
    @Test(expected = InvalidBetException.class)
    public void testBetAboveMaximum() throws InvalidBetException {
        bankroll.placeBet(2000.0);
    }
    
    @Test(expected = InvalidBetException.class)
    public void testNegativeBet() throws InvalidBetException {
        bankroll.placeBet(-10.0);
    }
    
    @Test
    public void testWinBet() throws InvalidBetException {
        bankroll.placeBet(10.0);
        bankroll.winBet();
        assertEquals(110.0, bankroll.getBalance(), 0.001);
        assertEquals(0.0, bankroll.getCurrentBet(), 0.001);
    }
    
    @Test
    public void testWinBlackjack() throws InvalidBetException {
        bankroll.placeBet(10.0);
        bankroll.winBlackjack();
        assertEquals(115.0, bankroll.getBalance(), 0.001); // 100 - 10 + 25 = 115
        assertEquals(0.0, bankroll.getCurrentBet(), 0.001);
    }
    
    @Test
    public void testLoseBet() throws InvalidBetException {
        bankroll.placeBet(10.0);
        bankroll.loseBet();
        assertEquals(90.0, bankroll.getBalance(), 0.001);
        assertEquals(0.0, bankroll.getCurrentBet(), 0.001);
    }
    
    @Test
    public void testPushBet() throws InvalidBetException {
        bankroll.placeBet(10.0);
        bankroll.pushBet();
        assertEquals(100.0, bankroll.getBalance(), 0.001);
        assertEquals(0.0, bankroll.getCurrentBet(), 0.001);
    }
    
    @Test
    public void testCanAffordMinBet() {
        assertTrue(bankroll.canAffordMinBet());
    }
    
    @Test
    public void testCannotAffordMinBet() {
        try {
            Bankroll poorBankroll = new Bankroll(0.5);
            fail("Should have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
    
    @Test
    public void testAddFunds() {
        bankroll.addFunds(50.0);
        assertEquals(150.0, bankroll.getBalance(), 0.001);
    }
    
    @Test
    public void testAddNegativeFunds() {
        bankroll.addFunds(-10.0);
        assertEquals(100.0, bankroll.getBalance(), 0.001);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testInsufficientInitialBalance() {
        new Bankroll(0.5);
    }
}
