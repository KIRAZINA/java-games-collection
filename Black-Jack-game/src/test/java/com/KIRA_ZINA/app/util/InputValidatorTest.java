package com.KIRA_ZINA.app.util;

import com.KIRA_ZINA.app.exception.InvalidPlayerInputException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for InputValidator class
 */
public class InputValidatorTest {
    
    @Test
    public void testValidHitInput() throws InvalidPlayerInputException {
        assertEquals("h", InputValidator.validateHitStandInput("h"));
        assertEquals("h", InputValidator.validateHitStandInput("hit"));
        assertEquals("h", InputValidator.validateHitStandInput("HIT"));
        assertEquals("h", InputValidator.validateHitStandInput("Hit"));
        
        assertEquals("s", InputValidator.validateHitStandInput("s"));
        assertEquals("s", InputValidator.validateHitStandInput("stand"));
        assertEquals("s", InputValidator.validateHitStandInput("STAND"));
        assertEquals("s", InputValidator.validateHitStandInput("Stand"));
    }
    
    @Test(expected = InvalidPlayerInputException.class)
    public void testInvalidHitInput() throws InvalidPlayerInputException {
        InputValidator.validateHitStandInput("invalid");
    }
    
    @Test(expected = InvalidPlayerInputException.class)
    public void testEmptyHitInput() throws InvalidPlayerInputException {
        InputValidator.validateHitStandInput("");
    }
    
    @Test(expected = InvalidPlayerInputException.class)
    public void testNullHitInput() throws InvalidPlayerInputException {
        InputValidator.validateHitStandInput(null);
    }
    
    @Test
    public void testValidYesNoInput() throws InvalidPlayerInputException {
        assertEquals("y", InputValidator.validateYesNoInput("y"));
        assertEquals("y", InputValidator.validateYesNoInput("yes"));
        assertEquals("y", InputValidator.validateYesNoInput("YES"));
        assertEquals("y", InputValidator.validateYesNoInput("Yes"));
        
        assertEquals("n", InputValidator.validateYesNoInput("n"));
        assertEquals("n", InputValidator.validateYesNoInput("no"));
        assertEquals("n", InputValidator.validateYesNoInput("NO"));
        assertEquals("n", InputValidator.validateYesNoInput("No"));
    }
    
    @Test(expected = InvalidPlayerInputException.class)
    public void testInvalidYesNoInput() throws InvalidPlayerInputException {
        InputValidator.validateYesNoInput("maybe");
    }
    
    @Test(expected = InvalidPlayerInputException.class)
    public void testEmptyYesNoInput() throws InvalidPlayerInputException {
        InputValidator.validateYesNoInput("");
    }
    
    @Test(expected = InvalidPlayerInputException.class)
    public void testNullYesNoInput() throws InvalidPlayerInputException {
        InputValidator.validateYesNoInput(null);
    }
    
    @Test
    public void testValidateNotEmpty() throws InvalidPlayerInputException {
        InputValidator.validateNotEmpty("valid", "Test field");
    }
    
    @Test(expected = InvalidPlayerInputException.class)
    public void testValidateEmptyString() throws InvalidPlayerInputException {
        InputValidator.validateNotEmpty("", "Test field");
    }
    
    @Test(expected = InvalidPlayerInputException.class)
    public void testValidateNullString() throws InvalidPlayerInputException {
        InputValidator.validateNotEmpty(null, "Test field");
    }
    
    @Test(expected = InvalidPlayerInputException.class)
    public void testValidateWhitespaceString() throws InvalidPlayerInputException {
        // Should fail as trim() is called in validateNotEmpty
        InputValidator.validateNotEmpty("   ", "Test field");
    }
}
