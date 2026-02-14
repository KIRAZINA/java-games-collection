package com.KIRA_ZINA.app.core;

import com.KIRA_ZINA.app.view.GameView;
import com.KIRA_ZINA.app.strategy.BasicDealerStrategy;
import com.KIRA_ZINA.app.strategy.ConservativeDealerStrategy;
import com.KIRA_ZINA.app.strategy.AggressiveDealerStrategy;
import com.KIRA_ZINA.app.strategy.DealerStrategy;
import com.KIRA_ZINA.app.exception.GameException;
import com.KIRA_ZINA.app.exception.InvalidPlayerInputException;
import com.KIRA_ZINA.app.exception.InvalidBetException;
import com.KIRA_ZINA.app.util.InputValidator;
import com.KIRA_ZINA.app.util.ConsoleColors;
import java.util.Scanner;

/**
 * Game controller - manages game flow and coordinates between model and view
 */
public class GameController {
    private GameModel model;
    private GameView view;
    private Scanner scanner;
    private static final double INITIAL_BALANCE = 100.0;
    
    public GameController() {
        this.model = new GameModel(INITIAL_BALANCE);
        this.view = new GameView();
        this.scanner = new Scanner(System.in);
        
        // Set default dealer strategy
        model.setDealerStrategy(new BasicDealerStrategy());
    }
    
    /**
     * Starts the main game loop
     */
    public void startGame() {
        view.displayWelcomeMessage();
        view.displayBankroll(model.getBankroll());
        
        boolean playAgain = true;
        while (playAgain && model.canContinuePlaying()) {
            try {
                playRound();
                if (model.canContinuePlaying()) {
                    playAgain = askPlayAgain();
                } else {
                    view.displayGameOver();
                    playAgain = false;
                }
            } catch (GameException e) {
                view.displayError("Game error: " + e.getMessage());
                break;
            }
        }
        
        view.displayGoodbyeMessage();
        scanner.close();
    }
    
    /**
     * Plays a single round of blackjack
     * @throws GameException if round fails
     */
    private void playRound() throws GameException {
        try {
            // Initialize round
            model.initializeRound();
            
            // Betting phase
            bettingPhase();
            
            // Display initial hands after betting
            view.displayHands(model, false);
            
            // Check for blackjacks
            if (checkForBlackjacks()) {
                return;
            }
            
            // Player's turn
            playerTurn();
            
            // If player didn't bust, dealer's turn
            if (!model.getPlayer().isBust()) {
                dealerTurn();
            }
            
            // Determine and display winner
            model.determineWinner();
            view.displayHands(model, true);
            view.displayWinner(model.getWinner());
            view.displayBankroll(model.getBankroll());
            
        } catch (GameException e) {
            throw e;
        } catch (Exception e) {
            throw new GameException("Unexpected error during round: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handles the betting phase
     * @throws GameException if betting fails
     */
    private void bettingPhase() throws GameException {
        view.displayBettingPhase();
        view.displayBankroll(model.getBankroll());
        
        while (true) {
            try {
                String input = view.promptBet();
                double betAmount = InputValidator.validateBetAmount(input, model.getBankroll());
                model.placeBet(betAmount);
                view.displayBetPlaced(betAmount);
                break;
            } catch (InvalidPlayerInputException | InvalidBetException e) {
                view.displayError(e.getMessage());
            }
        }
    }
    
    /**
     * Checks for initial blackjacks
     * @return true if blackjack occurred, false otherwise
     */
    private boolean checkForBlackjacks() {
        boolean playerBlackjack = model.getPlayer().hasBlackjack();
        boolean dealerBlackjack = model.getDealer().hasBlackjack();
        
        if (playerBlackjack || dealerBlackjack) {
            view.displayHands(model, true);
            
            if (playerBlackjack && dealerBlackjack) {
                view.displayWinner("tie");
            } else if (playerBlackjack) {
                view.displayBlackjack(true, true);
            } else {
                view.displayBlackjack(true, false);
            }
            view.displayBankroll(model.getBankroll());
            return true;
        }
        return false;
    }
    
    /**
     * Handles the player's turn
     * @throws GameException if player turn fails
     */
    private void playerTurn() throws GameException {
        while (!model.getPlayer().isBust()) {
            try {
                String input = view.promptHitOrStand();
                String choice = InputValidator.validateHitStandInput(input);
                
                if (choice.equals("h")) {
                    model.playerHit();
                    view.displayHands(model, false);
                    
                    if (model.getPlayer().isBust()) {
                        view.displayBust(true);
                        view.displayBankroll(model.getBankroll());
                        return;
                    }
                } else { // choice.equals("s")
                    break;
                }
            } catch (InvalidPlayerInputException e) {
                view.displayError(e.getMessage());
            }
        }
    }
    
    /**
     * Handles the dealer's turn
     * @throws GameException if dealer turn fails
     */
    private void dealerTurn() throws GameException {
        view.displayDealerTurnStart();
        
        try {
            while (model.shouldDealerHit()) {
                view.displayDealerAction("Dealer takes a card.");
                model.dealerHit();
                
                if (model.getDealer().isBust()) {
                    view.displayBust(false);
                    return;
                }
            }
            view.displayDealerStand();
        } catch (Exception e) {
            throw new GameException("Error during dealer turn: " + e.getMessage(), e);
        }
    }
    
    /**
     * Asks player if they want to play again
     * @return true if player wants to play again
     */
    private boolean askPlayAgain() {
        while (true) {
            try {
                String input = view.promptPlayAgain();
                String choice = InputValidator.validateYesNoInput(input);
                return choice.equals("y");
            } catch (InvalidPlayerInputException e) {
                view.displayError(e.getMessage());
            }
        }
    }
    
    /**
     * Sets the dealer strategy
     * @param strategy the new dealer strategy
     */
    public void setDealerStrategy(DealerStrategy strategy) {
        model.setDealerStrategy(strategy);
    }
    
    /**
     * Gets available dealer strategies
     * @return array of available strategies
     */
    public DealerStrategy[] getAvailableStrategies() {
        return new DealerStrategy[] {
            new BasicDealerStrategy(),
            new ConservativeDealerStrategy(),
            new AggressiveDealerStrategy()
        };
    }
}
