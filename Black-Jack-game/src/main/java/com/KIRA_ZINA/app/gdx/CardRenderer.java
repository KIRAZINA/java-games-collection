package com.KIRA_ZINA.app.gdx;

import com.KIRA_ZINA.app.model.Card;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

/**
 * Utility class for rendering playing cards programmatically.
 * Creates card textures on-the-fly with proper suit symbols and styling.
 */
public class CardRenderer {
    
    private static final int CARD_WIDTH = 500;
    private static final int CARD_HEIGHT = 726;
    private static final int CORNER_RADIUS = 30;
    private static final int BORDER_WIDTH = 8;
    
    /**
     * Creates a texture for a playing card
     */
    public static Texture createCardTexture(Card.Suit suit, Card.Rank rank) {
        Pixmap pixmap = new Pixmap(CARD_WIDTH, CARD_HEIGHT, Pixmap.Format.RGBA8888);
        
        // Draw white background with rounded corners
        pixmap.setColor(Color.WHITE);
        drawRoundedRect(pixmap, 0, 0, CARD_WIDTH, CARD_HEIGHT, CORNER_RADIUS);
        
        // Draw border
        Color borderColor = getSuitColor(suit);
        pixmap.setColor(borderColor);
        drawRoundedRectOutline(pixmap, BORDER_WIDTH / 2, BORDER_WIDTH / 2, 
                               CARD_WIDTH - BORDER_WIDTH, CARD_HEIGHT - BORDER_WIDTH, 
                               CORNER_RADIUS, BORDER_WIDTH);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        return texture;
    }
    
    /**
     * Creates a texture for the card back
     */
    public static Texture createCardBackTexture() {
        Pixmap pixmap = new Pixmap(CARD_WIDTH, CARD_HEIGHT, Pixmap.Format.RGBA8888);
        
        // Draw blue background with rounded corners
        pixmap.setColor(new Color(0.1f, 0.2f, 0.6f, 1f));
        drawRoundedRect(pixmap, 0, 0, CARD_WIDTH, CARD_HEIGHT, CORNER_RADIUS);
        
        // Draw decorative pattern
        pixmap.setColor(new Color(0.8f, 0.8f, 0.9f, 1f));
        for (int y = 50; y < CARD_HEIGHT - 50; y += 40) {
            for (int x = 50; x < CARD_WIDTH - 50; x += 40) {
                pixmap.fillCircle(x, y, 8);
            }
        }
        
        // Draw border
        pixmap.setColor(new Color(0.8f, 0.7f, 0.2f, 1f));
        drawRoundedRectOutline(pixmap, BORDER_WIDTH / 2, BORDER_WIDTH / 2, 
                               CARD_WIDTH - BORDER_WIDTH, CARD_HEIGHT - BORDER_WIDTH, 
                               CORNER_RADIUS, BORDER_WIDTH);
        
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        
        return texture;
    }
    
    /**
     * Draws text on a card texture (rank and suit symbols)
     */
    public static void drawCardText(SpriteBatch batch, BitmapFont font, Card card, 
                                    float x, float y, float cardWidth, float cardHeight) {
        Color suitColor = getSuitColor(card.getSuit());
        font.setColor(suitColor);
        
        String rankSymbol = getRankSymbol(card.getRank());
        String suitSymbol = getSuitSymbol(card.getSuit());
        
        // Draw rank in top-left corner
        font.getData().setScale(2.5f);
        font.draw(batch, rankSymbol, x + cardWidth * 0.1f, y + cardHeight * 0.95f);
        
        // Draw suit symbol below rank in corner
        font.getData().setScale(2.0f);
        font.draw(batch, suitSymbol, x + cardWidth * 0.1f, y + cardHeight * 0.85f);
        
        // Draw large suit symbol in center
        font.getData().setScale(6.0f);
        GlyphLayout layout = new GlyphLayout(font, suitSymbol);
        float centerX = x + (cardWidth - layout.width) / 2;
        float centerY = y + (cardHeight + layout.height) / 2;
        font.draw(batch, suitSymbol, centerX, centerY);
        
        // Draw rank in bottom-right corner (upside down effect)
        font.getData().setScale(2.5f);
        font.draw(batch, rankSymbol, x + cardWidth * 0.85f, y + cardHeight * 0.15f);
        
        // Draw suit symbol above rank in bottom corner
        font.getData().setScale(2.0f);
        font.draw(batch, suitSymbol, x + cardWidth * 0.85f, y + cardHeight * 0.25f);
        
        // Reset font scale
        font.getData().setScale(1.0f);
        font.setColor(Color.WHITE);
    }
    
    private static Color getSuitColor(Card.Suit suit) {
        return switch (suit) {
            case HEARTS, DIAMONDS -> new Color(0.8f, 0.1f, 0.1f, 1f); // Red
            case CLUBS, SPADES -> new Color(0.1f, 0.1f, 0.1f, 1f);    // Black
        };
    }
    
    private static String getRankSymbol(Card.Rank rank) {
        return switch (rank) {
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
    
    private static String getSuitSymbol(Card.Suit suit) {
        return switch (suit) {
            case HEARTS -> "♥";
            case DIAMONDS -> "♦";
            case CLUBS -> "♣";
            case SPADES -> "♠";
        };
    }
    
    private static void drawRoundedRect(Pixmap pixmap, int x, int y, int width, int height, int radius) {
        // Draw main rectangle
        pixmap.fillRectangle(x + radius, y, width - 2 * radius, height);
        pixmap.fillRectangle(x, y + radius, width, height - 2 * radius);
        
        // Draw corners
        pixmap.fillCircle(x + radius, y + radius, radius);
        pixmap.fillCircle(x + width - radius, y + radius, radius);
        pixmap.fillCircle(x + radius, y + height - radius, radius);
        pixmap.fillCircle(x + width - radius, y + height - radius, radius);
    }
    
    private static void drawRoundedRectOutline(Pixmap pixmap, int x, int y, int width, int height, 
                                               int radius, int thickness) {
        for (int i = 0; i < thickness; i++) {
            // Top line
            pixmap.drawLine(x + radius, y + i, x + width - radius, y + i);
            // Bottom line
            pixmap.drawLine(x + radius, y + height - i, x + width - radius, y + height - i);
            // Left line
            pixmap.drawLine(x + i, y + radius, x + i, y + height - radius);
            // Right line
            pixmap.drawLine(x + width - i, y + radius, x + width - i, y + height - radius);
            
            // Corners (simplified)
            pixmap.drawCircle(x + radius, y + radius, radius - i);
            pixmap.drawCircle(x + width - radius, y + radius, radius - i);
            pixmap.drawCircle(x + radius, y + height - radius, radius - i);
            pixmap.drawCircle(x + width - radius, y + height - radius, radius - i);
        }
    }
}
