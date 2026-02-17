package com.KIRA_ZINA.app.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TileTest {

    @Test
    void testTileCreation() {
        Tile tile = new Tile(2, 0, 0);
        assertEquals(2, tile.getValue());
        assertEquals(0, tile.getRow());
        assertEquals(0, tile.getCol());
        assertTrue(tile.isNew());
        assertFalse(tile.isMerging());
        assertEquals(1.0, tile.getScale());
        assertEquals(0.0, tile.getRotation());
        assertEquals(1.0f, tile.getOpacity());
    }

    @Test
    void testSetters() {
        Tile tile = new Tile(2, 0, 0);
        
        tile.setValue(4);
        assertEquals(4, tile.getValue());
        
        tile.setRow(1);
        tile.setCol(1);
        assertEquals(1, tile.getRow());
        assertEquals(1, tile.getCol());
        
        tile.setMerging(true);
        assertTrue(tile.isMerging());
        
        tile.setNew(false);
        assertFalse(tile.isNew());
        
        tile.setRotation(180.0);
        assertEquals(180.0, tile.getRotation());
        
        tile.setOpacity(0.5f);
        assertEquals(0.5f, tile.getOpacity());
    }

    @Test
    void testAnimationUpdate() {
        Tile tile = new Tile(2, 0, 0);
        tile.setAnimatedPosition(0, 0);
        tile.setTargetPosition(100, 100);
        
        // At 0% progress
        tile.updateAnimation(0.0);
        assertEquals(0.0, tile.getAnimatedX());
        assertEquals(0.0, tile.getAnimatedY());
        
        // At 50% progress
        tile.setAnimatedPosition(0, 0); // Reset start
        tile.setTargetPosition(100, 100); // Restore target
        tile.updateAnimation(0.5);
        assertEquals(50.0, tile.getAnimatedX(), 0.001);
        assertEquals(50.0, tile.getAnimatedY(), 0.001);
        
        // At 100% progress
        tile.setAnimatedPosition(0, 0);
        tile.setTargetPosition(100, 100); // Restore target
        tile.updateAnimation(1.0);
        assertEquals(100.0, tile.getAnimatedX(), 0.001);
        assertEquals(100.0, tile.getAnimatedY(), 0.001);
    }
}
