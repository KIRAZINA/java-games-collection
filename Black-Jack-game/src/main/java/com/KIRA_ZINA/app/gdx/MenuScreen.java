package com.KIRA_ZINA.app.gdx;

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
 * Menu screen for the Blackjack game.
 * Displays the game title and navigation buttons.
 */
public class MenuScreen implements Screen {
    
    private final BlackjackGdxGame game;
    private Stage stage;
    private Skin skin;
    private OrthographicCamera camera;
    
    public MenuScreen(BlackjackGdxGame game) {
        this.game = game;
        create();
    }
    
    private void create() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        
        // Create skin programmatically (fallback if uiskin.json not found)
        skin = createDefaultSkin();
        
        // Title label
        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = skin.getFont("default-font");
        titleStyle.fontColor = Color.GOLD;
        
        Label titleLabel = new Label("BLACKJACK", titleStyle);
        titleLabel.setFontScale(3f);
        titleLabel.setPosition(
            Gdx.graphics.getWidth() / 2f - titleLabel.getWidth() * 1.5f,
            Gdx.graphics.getHeight() * 0.7f
        );
        stage.addActor(titleLabel);
        
        // Subtitle
        Label.LabelStyle subtitleStyle = new Label.LabelStyle();
        subtitleStyle.font = skin.getFont("default-font");
        subtitleStyle.fontColor = Color.WHITE;
        
        Label subtitleLabel = new Label("LibGDX Edition", subtitleStyle);
        subtitleLabel.setFontScale(1.5f);
        subtitleLabel.setPosition(
            Gdx.graphics.getWidth() / 2f - subtitleLabel.getWidth() * 0.75f,
            Gdx.graphics.getHeight() * 0.6f
        );
        stage.addActor(subtitleLabel);
        
        // Play button
        TextButton playButton = new TextButton("PLAY", skin);
        playButton.setSize(200, 60);
        playButton.setPosition(
            Gdx.graphics.getWidth() / 2f - playButton.getWidth() / 2,
            Gdx.graphics.getHeight() * 0.4f
        );
        playButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game));
                dispose();
            }
        });
        stage.addActor(playButton);
        
        // Exit button
        TextButton exitButton = new TextButton("EXIT", skin);
        exitButton.setSize(200, 60);
        exitButton.setPosition(
            Gdx.graphics.getWidth() / 2f - exitButton.getWidth() / 2,
            Gdx.graphics.getHeight() * 0.25f
        );
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
        stage.addActor(exitButton);
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
        Gdx.gl.glClearColor(0, 0.3f, 0.1f, 1);
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
