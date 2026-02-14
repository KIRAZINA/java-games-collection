package com.KIRA_ZINA.app.gdx;

import com.KIRA_ZINA.app.model.Card;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AssetsTest {

    @Test
    public void testGetCardTexturePathForKnownCards() {
        assertEquals("cards/hearts_ace.png",
                Assets.getCardTexturePath(Card.Suit.HEARTS, Card.Rank.ACE));
        assertEquals("cards/spades_king.png",
                Assets.getCardTexturePath(Card.Suit.SPADES, Card.Rank.KING));
        assertEquals("cards/clubs_10.png",
                Assets.getCardTexturePath(Card.Suit.CLUBS, Card.Rank.TEN));
    }

    @Test
    public void testGetCardTexturePathForAllEnums() {
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                String path = Assets.getCardTexturePath(suit, rank);
                assertTrue(path.startsWith("cards/"));
                assertTrue(path.endsWith(".png"));
            }
        }
    }
}
