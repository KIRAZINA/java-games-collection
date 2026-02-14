package com.KIRA_ZINA.app.game;

import java.util.Scanner;
import com.KIRA_ZINA.app.model.*;
import com.KIRA_ZINA.app.entity.*;

/**
 * Main class for the Blackjack game
 */
public class BlackJackGame {
    private Deck deck;
    private Player player;
    private Dealer dealer;
    private Scanner scanner;

    public BlackJackGame() {
        deck = new Deck();
        player = new Player();
        dealer = new Dealer();
        scanner = new Scanner(System.in);
    }

    /**
     * Starts the blackjack game
     */
    public void startGame() {
        System.out.println("Welcome to Blackjack!");
        
        boolean playAgain = true;
        while (playAgain) {
            playRound();
            playAgain = askPlayAgain();
        }
        
        System.out.println("Thanks for playing!");
        scanner.close();
    }

    /**
     * Plays a single round of blackjack
     */
    private void playRound() {
        // Reset hands for new round
        player.clearHand();
        dealer.clearHand();
        
        // Shuffle deck if needed and deal initial cards
        deck.shuffle();
        
        // Set deck reference for dealer AI
        dealer.setDeck(deck);
        
        dealInitialCards();
        
        // Show initial hands - hide dealer info
        displayHands(false);
        
        // Check for blackjacks
        if (player.hasBlackjack() || dealer.hasBlackjack()) {
            determineWinner();
            return;
        }
        
        // Player's turn
        playerTurn();
        
        // If player didn't bust, dealer's turn
        if (!player.isBust()) {
            dealerTurn();
        }
        
        // Determine winner - this will show dealer's full hand
        determineWinner();
    }

    /**
     * Deals the initial two cards to player and dealer
     */
    private void dealInitialCards() {
        // Deal two cards to player
        player.addCardToHand(deck.dealCard());
        player.addCardToHand(deck.dealCard());
        
        // Deal two cards to dealer
        dealer.addCardToHand(deck.dealCard());
        dealer.addCardToHand(deck.dealCard());
    }

    /**
     * Handles the player's turn
     */
    private void playerTurn() {
        while (!player.isBust()) {
            System.out.print("Do you want to hit or stand? (h/s): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("h") || choice.equals("hit")) {
                player.addCardToHand(deck.dealCard());
                displayHands(false);
                
                if (player.isBust()) {
                    System.out.println("You busted! You lose.");
                    return;
                }
            } else if (choice.equals("s") || choice.equals("stand")) {
                break;
            } else {
                System.out.println("Invalid input. Please enter 'h' for hit or 's' for stand.");
            }
        }
    }

    /**
     * Handles the dealer's turn
     */
    private void dealerTurn() {
        System.out.println("\nDealer's turn:");
        
        // Dealer hits until hand value is 17 or higher
        while (dealer.shouldHit()) {
            System.out.println("Dealer takes a card.");
            dealer.addCardToHand(deck.dealCard());
            
            if (dealer.isBust()) {
                System.out.println("Dealer busted!");
                return;
            }
        }
        
        System.out.println("Dealer stops taking cards.");
    }

    /**
     * Displays the current hands of player and dealer
     * @param showDealerFullHand if true, shows dealer's full hand; if false, hides dealer info completely
     */
    private void displayHands(boolean showDealerFullHand) {
        System.out.println("\nYour hand: " + player.getHand() + " (Value: " + player.getHandValue() + ")");
        
        if (showDealerFullHand) {
            System.out.println("Dealer's hand: " + dealer.getHand() + " (Value: " + dealer.getHandValue() + ")");
        } else {
            // Completely hide dealer's cards and points during player turn
            System.out.println("Dealer's hand: [HIDDEN CARDS]");
        }
        System.out.println();
    }

    /**
     * Determines and announces the winner of the round
     */
    private void determineWinner() {
        // Show dealer's full hand before determining winner
        displayHands(true);
        
        int playerValue = player.getHandValue();
        int dealerValue = dealer.getHandValue();
        
        boolean playerHasBlackjack = player.hasBlackjack();
        boolean dealerHasBlackjack = dealer.hasBlackjack();
        
        // Check for blackjacks
        if (playerHasBlackjack && !dealerHasBlackjack) {
            System.out.println("Blackjack! You win!");
        } else if (dealerHasBlackjack && !playerHasBlackjack) {
            System.out.println("Dealer has blackjack! Dealer wins.");
        } else if (playerHasBlackjack && dealerHasBlackjack) {
            System.out.println("Both have blackjack! Push (tie).");
        }
        // Check if player busted
        else if (player.isBust()) {
            System.out.println("You busted! Dealer wins.");
        }
        // Check if dealer busted
        else if (dealer.isBust()) {
            System.out.println("Dealer busted! You win!");
        }
        // Compare values
        else if (playerValue > dealerValue) {
            System.out.println("You win!");
        } else if (dealerValue > playerValue) {
            System.out.println("Dealer wins.");
        } else { // Values are equal
            System.out.println("Push (tie).");
        }
    }

    /**
     * Asks the player if they want to play another round
     * @return true if player wants to play again, false otherwise
     */
    private boolean askPlayAgain() {
        while (true) {
            System.out.print("\nDo you want to play another round? (y/n): ");
            String choice = scanner.nextLine().trim().toLowerCase();
            
            if (choice.equals("y") || choice.equals("yes")) {
                return true;
            } else if (choice.equals("n") || choice.equals("no")) {
                return false;
            } else {
                System.out.println("Invalid input. Please enter 'y' for yes or 'n' for no.");
            }
        }
    }

    /**
     * Main method to start the game
     */
    public static void main(String[] args) {
        BlackJackGame game = new BlackJackGame();
        game.startGame();
    }
}