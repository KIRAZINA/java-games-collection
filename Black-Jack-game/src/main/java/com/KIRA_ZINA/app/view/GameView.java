package com.KIRA_ZINA.app.view;

import com.KIRA_ZINA.app.core.GameModel;
import com.KIRA_ZINA.app.entity.Player;
import com.KIRA_ZINA.app.entity.Dealer;
import com.KIRA_ZINA.app.model.Card;
import com.KIRA_ZINA.app.model.Bankroll;
import com.KIRA_ZINA.app.util.ConsoleColors;

/**
 * Game view - handles all display and user interaction with enhanced console interface
 */
public class GameView {
    private static final java.io.BufferedReader INPUT_READER =
            new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
    
    /**
     * Displays welcome message with colors
     */
    public void displayWelcomeMessage() {
        System.out.println(ConsoleColors.header(ConsoleColors.cyan("♠ ♥ BLACKJACK ♦ ♣")));
        System.out.println(ConsoleColors.yellow("Welcome to the Ultimate Blackjack Experience!"));
        System.out.println(ConsoleColors.cyan("Beat the dealer and win big!"));
        System.out.println();
    }
    
    /**
     * Displays goodbye message
     */
    public void displayGoodbyeMessage() {
        System.out.println("\n" + ConsoleColors.header(ConsoleColors.cyan("THANKS FOR PLAYING!")));
        System.out.println(ConsoleColors.yellow("Come back soon for more blackjack action!"));
    }
    
    /**
     * Displays current bankroll with colors
     * @param bankroll player's bankroll
     */
    public void displayBankroll(Bankroll bankroll) {
        String balanceText = String.format("Balance: $%.2f", bankroll.getBalance());
        String betText = String.format("Current Bet: $%.2f", bankroll.getCurrentBet());
        
        System.out.println(ConsoleColors.green("$ " + balanceText));
        if (bankroll.getCurrentBet() > 0) {
            System.out.println(ConsoleColors.yellow("* " + betText));
        }
        System.out.println();
    }
    
    /**
     * Displays betting phase header
     */
    public void displayBettingPhase() {
        System.out.println(ConsoleColors.header(ConsoleColors.yellow("BETTING PHASE")));
    }
    
    /**
     * Prompts player for bet amount
     * @return player's bet input
     */
    public String promptBet() {
        System.out.print(ConsoleColors.cyan("Enter your bet amount: $"));
        return readLineSafe();
    }
    
    /**
     * Displays bet confirmation
     * @param amount bet amount
     */
    public void displayBetPlaced(double amount) {
        System.out.println(ConsoleColors.green(String.format("+ Bet placed: $%.2f", amount)));
        System.out.println();
    }
    
    /**
     * Displays game over message
     */
    public void displayGameOver() {
        System.out.println("\n" + ConsoleColors.red("X GAME OVER - Insufficient Funds"));
        System.out.println(ConsoleColors.yellow("You're out of money! Better luck next time."));
    }
    
    /**
     * Displays current hands of player and dealer with enhanced formatting
     * @param model game model containing current state
     * @param showDealerFullHand if true, shows dealer's full hand
     */
    public void displayHands(GameModel model, boolean showDealerFullHand) {
        Player player = model.getPlayer();
        Dealer dealer = model.getDealer();
        
        System.out.println(ConsoleColors.separator('-', 50));
        
        // Player hand
        String playerHand = String.format("Your hand: %s", player.getHand());
        String playerValue = String.format("(Value: %d)", player.getHandValue());
        System.out.println(ConsoleColors.green(playerHand) + " " + ConsoleColors.bold(playerValue));
        
        // Dealer hand
        if (showDealerFullHand) {
            String dealerHand = String.format("Dealer's hand: %s", dealer.getHand());
            String dealerValue = String.format("(Value: %d)", dealer.getHandValue());
            System.out.println(ConsoleColors.red(dealerHand) + " " + ConsoleColors.bold(dealerValue));
        } else {
            System.out.println(ConsoleColors.red("Dealer's hand: [HIDDEN CARDS]"));
        }
        
        System.out.println(ConsoleColors.separator('-', 50));
        System.out.println();
    }
    
    /**
     * Displays dealer action message with colors
     * @param action the action dealer took
     */
    public void displayDealerAction(String action) {
        System.out.println(ConsoleColors.yellow("* " + action));
    }
    
    /**
     * Displays the winner of the round with celebration
     * @param winner the winner ("player", "dealer", or "tie")
     */
    public void displayWinner(String winner) {
        System.out.println(ConsoleColors.separator('*', 30));
        switch (winner.toLowerCase()) {
            case "player":
                System.out.println(ConsoleColors.green("* YOU WIN! *"));
                System.out.println(ConsoleColors.green("Congratulations!"));
                break;
            case "dealer":
                System.out.println(ConsoleColors.red("Dealer Wins"));
                System.out.println(ConsoleColors.yellow("Better luck next time!"));
                break;
            case "tie":
                System.out.println(ConsoleColors.yellow("PUSH (TIE)"));
                System.out.println(ConsoleColors.cyan("It's a stand-off!"));
                break;
            default:
                System.out.println("Unknown result.");
        }
        System.out.println(ConsoleColors.separator('*', 30));
    }
    
    /**
     * Displays blackjack message with special formatting
     * @param hasBlackjack true if blackjack occurred
     * @param isPlayer true if it's player's blackjack
     */
    public void displayBlackjack(boolean hasBlackjack, boolean isPlayer) {
        if (hasBlackjack) {
            if (isPlayer) {
                System.out.println(ConsoleColors.green("* BLACKJACK! You win! *"));
                System.out.println(ConsoleColors.green("Perfect 21!"));
            } else {
                System.out.println(ConsoleColors.red("X Dealer has BLACKJACK!"));
                System.out.println(ConsoleColors.red("Dealer wins with 21!"));
            }
        }
    }
    
    /**
     * Displays bust message with dramatic effect
     * @param isPlayer true if player busted, false if dealer busted
     */
    public void displayBust(boolean isPlayer) {
        if (isPlayer) {
            System.out.println(ConsoleColors.red("X BUST! You went over 21!"));
            System.out.println(ConsoleColors.red("You lose this round!"));
        } else {
            System.out.println(ConsoleColors.green("* Dealer BUST!"));
            System.out.println(ConsoleColors.green("Dealer went over 21 - You win!"));
        }
    }
    
    /**
     * Prompts player for hit/stand decision with enhanced formatting
     * @return the player's choice
     */
    public String promptHitOrStand() {
        System.out.print(ConsoleColors.cyan("Do you want to ") + ConsoleColors.green("hit") + 
                          ConsoleColors.cyan(" or ") + ConsoleColors.red("stand") + 
                          ConsoleColors.cyan("? (h/s): "));
        return readLineSafe();
    }
    
    /**
     * Prompts player to play again with enhanced formatting
     * @return the player's choice
     */
    public String promptPlayAgain() {
        System.out.print(ConsoleColors.cyan("Play another round? (") + 
                          ConsoleColors.green("y") + ConsoleColors.cyan("/") + 
                          ConsoleColors.red("n") + ConsoleColors.cyan("): "));
        return readLineSafe();
    }
    
    /**
     * Displays error message with red color
     * @param message the error message
     */
    public void displayError(String message) {
        System.out.println(ConsoleColors.red("X Error: " + message));
    }
    
    /**
     * Displays dealer turn start message
     */
    public void displayDealerTurnStart() {
        System.out.println("\n" + ConsoleColors.yellow("Dealer's Turn:"));
        System.out.println(ConsoleColors.cyan("Let's see what the dealer does..."));
    }
    
    /**
     * Displays dealer stand message
     */
    public void displayDealerStand() {
        System.out.println(ConsoleColors.yellow("X Dealer stops taking cards."));
    }

    private String readLineSafe() {
        try {
            return INPUT_READER.readLine();
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
