package com.KIRA_ZINA.app.gdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.files.FileHandle;

/**
 * Asset management class for loading and storing game resources.
 * Handles card textures, UI skins, and other game assets.
 */
public class Assets {
    
    // UI Skin
    public static final String UI_SKIN = "skin/uiskin.json";
    
    // Card textures path pattern
    public static final String CARDS_FOLDER = "cards/";
    public static final String CARD_BACK = "cards/back.png";
    
    // Background
    public static final String TABLE_BACKGROUND = "table/table_bg.png";
    
    // Card suit and rank names for file naming
    private static final String[] SUIT_NAMES = {"hearts", "diamonds", "clubs", "spades"};
    private static final String[] RANK_NAMES = {"ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "jack", "queen", "king"};
    
    /**
     * Loads all game assets into the AssetManager
     */
    public static void load(AssetManager assetManager) {
        // Load optional assets only when files exist.
        loadIfExists(assetManager, CARD_BACK, Texture.class);
        loadIfExists(assetManager, TABLE_BACKGROUND, Texture.class);
        
        // Load all card textures
        for (String suit : SUIT_NAMES) {
            for (String rank : RANK_NAMES) {
                String cardPath = CARDS_FOLDER + suit + "_" + rank + ".png";
                loadIfExists(assetManager, cardPath, Texture.class);
            }
        }
    }
    
    /**
     * Gets the texture path for a specific card
     * @param suit The suit of the card (HEARTS, DIAMONDS, CLUBS, SPADES)
     * @param rank The rank of the card (ACE, TWO, THREE, etc.)
     * @return The path to the card texture
     */
    public static String getCardTexturePath(com.KIRA_ZINA.app.model.Card.Suit suit, 
                                            com.KIRA_ZINA.app.model.Card.Rank rank) {
        String suitName = SUIT_NAMES[suit.ordinal()];
        String rankName = RANK_NAMES[rank.ordinal()];
        return CARDS_FOLDER + suitName + "_" + rankName + ".png";
    }

    private static <T> void loadIfExists(AssetManager assetManager, String path, Class<T> assetType) {
        FileHandle handle = Gdx.files.internal(path);
        if (handle.exists()) {
            assetManager.load(path, assetType);
        }
    }
    
    /**
     * Gets a card texture from the asset manager
     * @param assetManager The asset manager
     * @param suit The suit of the card
     * @param rank The rank of the card
     * @return The texture for the card, or null if not found
     */
    public static Texture getCardTexture(AssetManager assetManager, 
                                         com.KIRA_ZINA.app.model.Card.Suit suit,
                                         com.KIRA_ZINA.app.model.Card.Rank rank) {
        String path = getCardTexturePath(suit, rank);
        if (assetManager.isLoaded(path)) {
            return assetManager.get(path, Texture.class);
        }
        return null;
    }
    
    /**
     * Gets the card back texture
     * @param assetManager The asset manager
     * @return The card back texture, or null if not found
     */
    public static Texture getCardBackTexture(AssetManager assetManager) {
        if (assetManager.isLoaded(CARD_BACK)) {
            return assetManager.get(CARD_BACK, Texture.class);
        }
        return null;
    }
    
    /**
     * Gets the table background texture
     * @param assetManager The asset manager
     * @return The table background texture, or null if not found
     */
    public static Texture getTableBackground(AssetManager assetManager) {
        if (assetManager.isLoaded(TABLE_BACKGROUND)) {
            return assetManager.get(TABLE_BACKGROUND, Texture.class);
        }
        return null;
    }
}
