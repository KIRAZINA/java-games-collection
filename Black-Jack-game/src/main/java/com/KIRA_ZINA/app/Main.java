package com.KIRA_ZINA.app;

import com.KIRA_ZINA.app.core.GameController;
import com.KIRA_ZINA.app.strategy.BasicDealerStrategy;
import com.KIRA_ZINA.app.strategy.ConservativeDealerStrategy;
import com.KIRA_ZINA.app.strategy.AggressiveDealerStrategy;

/**
 * Main entry point for the Blackjack game application
 */
public class Main {
    public static void main(String[] args) {
        GameController game = new GameController();
        game.startGame();
    }
}
