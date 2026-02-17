package com.KIRA_ZINA.app.gdx;

import com.KIRA_ZINA.app.strategy.AggressiveDealerStrategy;
import com.KIRA_ZINA.app.strategy.BasicDealerStrategy;
import com.KIRA_ZINA.app.strategy.ConservativeDealerStrategy;
import com.KIRA_ZINA.app.strategy.DealerStrategy;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

/**
 * Difficulty selection screen for choosing dealer strategy.
 * Allows players to select between Basic, Conservative, and Aggressive dealer modes.
 */
public class DifficultySelectionScreen implements Screen {
    
    private final BlackjackGdxGame game;
    private Stage stage;
    private Skin skin;
    private OrthographicCamera camera;
    
    public DifficultySelectionScreen(BlackjackGdxGame game) {
        this.game = game;
        create();
    }
    
    private void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        
        skin = createDefaultSkin();
        
        // Title label
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = skin.getFont("default-font");
        titleStyle.fontColor = Color.GOLD;
        
        Label titleLabel = new Label("SELECT DIFFICULTY", titleStyle);
        titleLabel.setFontScale(2.5f);
        titleLabel.setPosition(
            Gdx.graphics.getWidth() / 2f - titleLabel.getWidth() * 1.25f,
            Gdx.graphics.getHeight() * 0.8f
        );
        stage.addActor(titleLabel);
        
        // Subtitle
        Label.LabelStyle subtitleStyle = new Label.LabelStyle();
        subtitleStyle.font = skin.getFont("default-font");
        subtitleStyle.fontColor = Color.WHITE;
        
        Label subtitleLabel = new Label("Choose dealer strategy", subtitleStyle);
        subtitleLabel.setFontScale(1.2f);
        subtitleLabel.setPosition(
            Gdx.graphics.getWidth() / 2f - subtitleLabel.getWidth() * 0.6f,
            Gdx.graphics.getHeight() * 0.72f
        );
        stage.addActor(subtitleLabel);
        
        // Basic difficulty button
        TextButton basicButton = new TextButton("BASIC", skin);
        basicButton.setSize(300, 70);
        basicButton.setPosition(
            Gdx.graphics.getWidth() / 2f - basicButton.getWidth() / 2,
            Gdx.graphics.getHeight() * 0.55f
        );
        basicButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startGameWithDifficulty(new BasicDealerStrategy());
            }
        });
        stage.addActor(basicButton);
        
        // Basic description
        Label basicDesc = new Label("Standard casino rules\nHits until 17, stands on 17+", subtitleStyle);
        basicDesc.setFontScale(0.9f);
        basicDesc.setPosition(
            Gdx.graphics.getWidth() / 2f - 140,
            Gdx.graphics.getHeight() * 0.48f
        );
        basicDesc.setColor(Color.LIGHT_GRAY);
        stage.addActor(basicDesc);
        
        // Conservative difficulty button
        TextButton conservativeButton = new TextButton("CONSERVATIVE", skin);
        conservativeButton.setSize(300, 70);
        conservativeButton.setPosition(
            Gdx.graphics.getWidth() / 2f - conservativeButton.getWidth() / 2,
            Gdx.graphics.getHeight() * 0.38f
        );
        conservativeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startGameWithDifficulty(new ConservativeDealerStrategy());
            }
        });
        stage.addActor(conservativeButton);
        
        // Conservative description
        Label conservativeDesc = new Label("Easier difficulty\nDealer plays cautiously, stands on 16+", subtitleStyle);
        conservativeDesc.setFontScale(0.9f);
        conservativeDesc.setPosition(
            Gdx.graphics.getWidth() / 2f - 170,
            Gdx.graphics.getHeight() * 0.31f
        );
        conservativeDesc.setColor(Color.LIGHT_GRAY);
        stage.addActor(conservativeDesc);
        
        // Aggressive difficulty button
        TextButton aggressiveButton = new TextButton("AGGRESSIVE", skin);
        aggressiveButton.setSize(300, 70);
        aggressiveButton.setPosition(
            Gdx.graphics.getWidth() / 2f - aggressiveButton.getWidth() / 2,
            Gdx.graphics.getHeight() * 0.21f
        );
        aggressiveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startGameWithDifficulty(new AggressiveDealerStrategy());
            }
        });
        stage.addActor(aggressiveButton);
        
        // Aggressive description
        Label aggressiveDesc = new Label("Harder difficulty\nDealer takes risks, hits until 18", subtitleStyle);
        aggressiveDesc.setFontScale(0.9f);
        aggressiveDesc.setPosition(
            Gdx.graphics.getWidth() / 2f - 160,
            Gdx.graphics.getHeight() * 0.14f
        );
        aggressiveDesc.setColor(Color.LIGHT_GRAY);
        stage.addActor(aggressiveDesc);
        
        // Back button
        TextButton backButton = new TextButton("BACK", skin);
        backButton.setSize(120, 50);
        backButton.setPosition(20, 20);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MenuScreen(game));
                dispose();
            }
        });
        stage.addActor(backButton);
    }
    
    private void startGameWithDifficulty(DealerStrategy strategy) {
        game.setScreen(new GameScreen(game, strategy));
        dispose();
    }
    
    /**
     * Creates a default skin programmatically
     */
    private Skin createDefaultSkin() {
        Skin skin = new Skin();
        
        // Create default font
        BitmapFont font = new BitmapFont();
        font.getData().setScale(1.5f);
        skin.add("default-font", font);
        
        // Create button style
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        buttonStyle.fontColor = Color.WHITE;
        buttonStyle.downFontColor = Color.GOLD;
        buttonStyle.overFontColor = Color.YELLOW;
        skin.add("default", buttonStyle);
        
        // Create label style
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
        // Clear screen with dark green (casino table color)
        Gdx.gl.glClearColor(0, 0.35f, 0.12f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        camera.update();
        
        stage.act(delta);
        stage.draw();
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
