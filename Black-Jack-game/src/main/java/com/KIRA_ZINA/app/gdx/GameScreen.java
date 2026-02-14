package com.KIRA_ZINA.app.gdx;

import com.KIRA_ZINA.app.core.GameModel;
import com.KIRA_ZINA.app.exception.GameException;
import com.KIRA_ZINA.app.exception.InvalidCardOperationException;
import com.KIRA_ZINA.app.model.Card;
import com.KIRA_ZINA.app.strategy.BasicDealerStrategy;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Main game screen for Blackjack.
 * Uses the same game model as console mode: betting, bankroll, outcomes.
 */
public class GameScreen implements Screen {

    private static final double INITIAL_BALANCE = 100.0;
    private static final double BET_STEP = 5.0;
    private static final double DEFAULT_BET = 10.0;

    private static final float CARD_WIDTH = 80;
    private static final float CARD_HEIGHT = 120;
    private static final float CARD_SPACING = 90;
    private static final float PLAYER_CARDS_Y = 100;
    private static final float DEALER_CARDS_Y = 450;

    private final BlackjackGdxGame game;
    private Stage stage;
    private Skin skin;
    private OrthographicCamera camera;
    private SpriteBatch batch;

    private GameModel model;
    private GameState gameState;
    private double selectedBet;

    private Label bankrollLabel;
    private Label betLabel;
    private Label playerScoreLabel;
    private Label dealerScoreLabel;
    private Label resultLabel;
    private Label statusLabel;

    private TextButton betMinusButton;
    private TextButton betPlusButton;
    private TextButton dealButton;
    private TextButton hitButton;
    private TextButton standButton;
    private TextButton newGameButton;
    private TextButton menuButton;

    private enum GameState {
        BETTING_PHASE,
        PLAYER_TURN,
        DEALER_TURN,
        ROUND_END
    }

    public GameScreen(BlackjackGdxGame game) {
        this.game = game;
        this.batch = game.getBatch();
        this.selectedBet = DEFAULT_BET;
        create();
        initializeModel();
        prepareRound();
    }

    private void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        skin = createDefaultSkin();
        createUI();
    }

    private void initializeModel() {
        model = new GameModel(INITIAL_BALANCE);
        model.setDealerStrategy(new BasicDealerStrategy());
    }

    private void createUI() {
        bankrollLabel = new Label("Balance: $0.00", skin);
        bankrollLabel.setPosition(20, Gdx.graphics.getHeight() - 45);
        bankrollLabel.setColor(Color.WHITE);
        stage.addActor(bankrollLabel);

        betLabel = new Label("Bet: $0.00", skin);
        betLabel.setPosition(20, Gdx.graphics.getHeight() - 85);
        betLabel.setColor(Color.GOLD);
        stage.addActor(betLabel);

        playerScoreLabel = new Label("Player: 0", skin);
        playerScoreLabel.setPosition(20, PLAYER_CARDS_Y + CARD_HEIGHT + 20);
        playerScoreLabel.setColor(Color.WHITE);
        stage.addActor(playerScoreLabel);

        dealerScoreLabel = new Label("Dealer: ?", skin);
        dealerScoreLabel.setPosition(20, DEALER_CARDS_Y + CARD_HEIGHT + 20);
        dealerScoreLabel.setColor(Color.WHITE);
        stage.addActor(dealerScoreLabel);

        statusLabel = new Label("", skin);
        statusLabel.setPosition(Gdx.graphics.getWidth() / 2f - 240, Gdx.graphics.getHeight() / 2f);
        statusLabel.setColor(Color.YELLOW);
        stage.addActor(statusLabel);

        resultLabel = new Label("", skin);
        resultLabel.setPosition(Gdx.graphics.getWidth() / 2f - 240, Gdx.graphics.getHeight() / 2f + 50);
        resultLabel.setColor(Color.GOLD);
        resultLabel.setFontScale(1.2f);
        stage.addActor(resultLabel);

        betMinusButton = new TextButton("BET -", skin);
        betMinusButton.setSize(120, 45);
        betMinusButton.setPosition(Gdx.graphics.getWidth() / 2f - 220, 20);
        betMinusButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (gameState == GameState.BETTING_PHASE) {
                    changeBet(-BET_STEP);
                }
            }
        });
        stage.addActor(betMinusButton);

        betPlusButton = new TextButton("BET +", skin);
        betPlusButton.setSize(120, 45);
        betPlusButton.setPosition(Gdx.graphics.getWidth() / 2f - 80, 20);
        betPlusButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (gameState == GameState.BETTING_PHASE) {
                    changeBet(BET_STEP);
                }
            }
        });
        stage.addActor(betPlusButton);

        dealButton = new TextButton("DEAL", skin);
        dealButton.setSize(120, 45);
        dealButton.setPosition(Gdx.graphics.getWidth() / 2f + 60, 20);
        dealButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (gameState == GameState.BETTING_PHASE) {
                    placeBetAndDeal();
                }
            }
        });
        stage.addActor(dealButton);

        hitButton = new TextButton("HIT", skin);
        hitButton.setSize(120, 50);
        hitButton.setPosition(Gdx.graphics.getWidth() / 2f - 140, 20);
        hitButton.setVisible(false);
        hitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (gameState == GameState.PLAYER_TURN) {
                    playerHit();
                }
            }
        });
        stage.addActor(hitButton);

        standButton = new TextButton("STAND", skin);
        standButton.setSize(120, 50);
        standButton.setPosition(Gdx.graphics.getWidth() / 2f + 20, 20);
        standButton.setVisible(false);
        standButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (gameState == GameState.PLAYER_TURN) {
                    playerStand();
                }
            }
        });
        stage.addActor(standButton);

        newGameButton = new TextButton("NEXT ROUND", skin);
        newGameButton.setSize(180, 50);
        newGameButton.setPosition(Gdx.graphics.getWidth() / 2f - 90, 80);
        newGameButton.setVisible(false);
        newGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (model.canContinuePlaying()) {
                    prepareRound();
                } else {
                    initializeModel();
                    prepareRound();
                }
            }
        });
        stage.addActor(newGameButton);

        menuButton = new TextButton("MENU", skin);
        menuButton.setSize(100, 40);
        menuButton.setPosition(Gdx.graphics.getWidth() - 120, Gdx.graphics.getHeight() - 50);
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
                dispose();
            }
        });
        stage.addActor(menuButton);
    }

    private void prepareRound() {
        try {
            model.initializeRound();
            gameState = GameState.BETTING_PHASE;
            resultLabel.setText("");
            statusLabel.setText("Choose bet and press DEAL.");
            selectedBet = clampBet(selectedBet);
            setBettingControlsVisible(true);
            setActionControlsVisible(false);
            newGameButton.setVisible(false);
            updateScoreDisplay(true);
            updateBankrollDisplay();
        } catch (GameException e) {
            statusLabel.setText("Failed to initialize round.");
            resultLabel.setText(e.getMessage());
            gameState = GameState.ROUND_END;
            setBettingControlsVisible(false);
            setActionControlsVisible(false);
            newGameButton.setVisible(true);
            newGameButton.setText("RESTART");
        }
    }

    private void changeBet(double delta) {
        selectedBet = clampBet(selectedBet + delta);
        updateBankrollDisplay();
    }

    private double clampBet(double value) {
        double min = model.getBankroll().getMinBet();
        double max = Math.min(model.getBankroll().getMaxBet(), model.getBankroll().getBalance());
        if (max < min) {
            return min;
        }
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private void placeBetAndDeal() {
        try {
            selectedBet = clampBet(selectedBet);
            model.placeBet(selectedBet);
            gameState = GameState.PLAYER_TURN;
            statusLabel.setText("Your turn: hit or stand?");
            resultLabel.setText("");
            setBettingControlsVisible(false);
            setActionControlsVisible(true);
            updateScoreDisplay(true);
            updateBankrollDisplay();

            if (model.getPlayer().hasBlackjack() || model.getDealer().hasBlackjack()) {
                endRound();
            }
        } catch (GameException e) {
            statusLabel.setText("Bet failed.");
            resultLabel.setText(e.getMessage());
            updateBankrollDisplay();
        }
    }

    private void playerHit() {
        try {
            model.playerHit();
            updateScoreDisplay(true);
            if (model.getPlayer().isBust()) {
                endRound();
            } else if (model.getPlayer().getHandValue() == 21) {
                playerStand();
            }
        } catch (InvalidCardOperationException e) {
            statusLabel.setText("Card deal error.");
            resultLabel.setText(e.getMessage());
            endRound();
        }
    }

    private void playerStand() {
        gameState = GameState.DEALER_TURN;
        statusLabel.setText("Dealer's turn...");
        setActionControlsVisible(false);
        dealerPlay();
    }

    private void dealerPlay() {
        try {
            while (model.shouldDealerHit()) {
                model.dealerHit();
            }
            endRound();
        } catch (Exception e) {
            statusLabel.setText("Dealer action failed.");
            resultLabel.setText(e.getMessage());
            endRound();
        }
    }

    private void endRound() {
        gameState = GameState.ROUND_END;
        model.determineWinner();
        updateScoreDisplay(false);
        updateBankrollDisplay();
        resultLabel.setText(buildResultMessage());

        if (model.canContinuePlaying()) {
            statusLabel.setText("Round complete.");
            newGameButton.setText("NEXT ROUND");
        } else {
            statusLabel.setText("No funds left.");
            newGameButton.setText("RESTART");
        }
        newGameButton.setVisible(true);
        setBettingControlsVisible(false);
        setActionControlsVisible(false);
    }

    private String buildResultMessage() {
        boolean playerHasBlackjack = model.getPlayer().hasBlackjack();
        boolean dealerHasBlackjack = model.getDealer().hasBlackjack();
        if (playerHasBlackjack && !dealerHasBlackjack) {
            return "BLACKJACK! You win 3:2.";
        }
        if (dealerHasBlackjack && !playerHasBlackjack) {
            return "Dealer has Blackjack.";
        }
        if (playerHasBlackjack && dealerHasBlackjack) {
            return "Push. Both have Blackjack.";
        }
        if (model.getPlayer().isBust()) {
            return "You busted. Dealer wins.";
        }
        if (model.getDealer().isBust()) {
            return "Dealer busted. You win.";
        }
        return switch (model.getWinner()) {
            case "player" -> "You win.";
            case "dealer" -> "Dealer wins.";
            default -> "Push (tie).";
        };
    }

    private void setBettingControlsVisible(boolean visible) {
        betMinusButton.setVisible(visible);
        betPlusButton.setVisible(visible);
        dealButton.setVisible(visible);
    }

    private void setActionControlsVisible(boolean visible) {
        hitButton.setVisible(visible);
        standButton.setVisible(visible);
    }

    private void updateBankrollDisplay() {
        bankrollLabel.setText(String.format("Balance: $%.2f", model.getBankroll().getBalance()));
        betLabel.setText(String.format("Bet: $%.2f  (Current: $%.2f)",
                selectedBet, model.getBankroll().getCurrentBet()));
    }

    private void updateScoreDisplay(boolean hideDealerCard) {
        playerScoreLabel.setText("Player: " + model.getPlayer().getHandValue());

        if (hideDealerCard && model.getDealer().getHand().getCards().size() > 0) {
            dealerScoreLabel.setText("Dealer: " + model.getDealer().getVisibleCardValue() + " + ?");
        } else {
            dealerScoreLabel.setText("Dealer: " + model.getDealer().getHandValue());
        }
    }

    private Skin createDefaultSkin() {
        Skin skin = new Skin();

        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        skin.add("default-font", font);

        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = Color.GOLD;
        buttonStyle.overFontColor = Color.YELLOW;
        skin.add("default", buttonStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        skin.add("default", labelStyle);

        return skin;
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0.4f, 0.15f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        drawCards();
        batch.end();

        stage.act(delta);
        stage.draw();
    }

    private void drawCards() {
        float screenWidth = Gdx.graphics.getWidth();

        float playerStartX = screenWidth / 2f
                - ((model.getPlayer().getHand().getCards().size() - 1) * CARD_SPACING) / 2f;
        for (int i = 0; i < model.getPlayer().getHand().getCards().size(); i++) {
            Card card = model.getPlayer().getHand().getCards().get(i);
            drawCard(card, playerStartX + i * CARD_SPACING, PLAYER_CARDS_Y, true);
        }

        float dealerStartX = screenWidth / 2f
                - ((model.getDealer().getHand().getCards().size() - 1) * CARD_SPACING) / 2f;
        for (int i = 0; i < model.getDealer().getHand().getCards().size(); i++) {
            Card card = model.getDealer().getHand().getCards().get(i);
            boolean showCard = (gameState != GameState.PLAYER_TURN || i == 0);
            drawCard(card, dealerStartX + i * CARD_SPACING, DEALER_CARDS_Y, showCard);
        }
    }

    private void drawCard(Card card, float x, float y, boolean faceUp) {
        Texture texture;
        if (faceUp) {
            texture = Assets.getCardTexture(game.getAssetManager(), card.getSuit(), card.getRank());
        } else {
            texture = Assets.getCardBackTexture(game.getAssetManager());
        }

        if (texture != null) {
            batch.draw(texture, x, y, CARD_WIDTH, CARD_HEIGHT);
        } else {
            drawPlaceholderCard(x, y, faceUp, card);
        }
    }

    private void drawPlaceholderCard(float x, float y, boolean faceUp, Card card) {
        batch.setColor(Color.WHITE);
        BitmapFont font = skin.getFont("default-font");
        font.setColor(faceUp ? Color.BLACK : Color.WHITE);

        String cardText;
        if (faceUp && card != null) {
            cardText = getCardSymbol(card) + "\n" + getSuitSymbol(card.getSuit());
        } else {
            cardText = "???";
        }
        font.draw(batch, cardText, x + 15, y + CARD_HEIGHT - 30);
    }

    private String getCardSymbol(Card card) {
        return switch (card.getRank()) {
            case ACE -> "A";
            case TWO -> "2";
            case THREE -> "3";
            case FOUR -> "4";
            case FIVE -> "5";
            case SIX -> "6";
            case SEVEN -> "7";
            case EIGHT -> "8";
            case NINE -> "9";
            case TEN -> "10";
            case JACK -> "J";
            case QUEEN -> "Q";
            case KING -> "K";
        };
    }

    private String getSuitSymbol(Card.Suit suit) {
        return switch (suit) {
            case HEARTS -> "H";
            case DIAMONDS -> "D";
            case CLUBS -> "C";
            case SPADES -> "S";
        };
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        camera.setToOrtho(false, width, height);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
    }
}
