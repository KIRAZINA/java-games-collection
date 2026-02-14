package com.KIRA_ZINA.app.gdx;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Main LibGDX game class for Blackjack.
 * Manages screens, assets, and the game lifecycle.
 */
public class BlackjackGdxGame extends Game {
    
    private SpriteBatch batch;
    private AssetManager assetManager;
    
    @Override
    public void create() {
        batch = new SpriteBatch();
        assetManager = new AssetManager();
        
        // Load assets
        Assets.load(assetManager);
        assetManager.finishLoading();
        
        // Set the initial screen to menu
        setScreen(new MenuScreen(this));
    }
    
    @Override
    public void render() {
        super.render();
    }
    
    @Override
    public void dispose() {
        batch.dispose();
        assetManager.dispose();
        super.dispose();
    }
    
    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }
    
    @Override
    public void pause() {
        super.pause();
    }
    
    @Override
    public void resume() {
        super.resume();
    }
    
    /**
     * Gets the SpriteBatch for rendering
     */
    public SpriteBatch getBatch() {
        return batch;
    }
    
    /**
     * Gets the AssetManager for loading resources
     */
    public AssetManager getAssetManager() {
        return assetManager;
    }
}