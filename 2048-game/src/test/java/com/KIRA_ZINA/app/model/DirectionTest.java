package com.KIRA_ZINA.app.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DirectionTest {

    @Test
    void testEnumValues() {
        assertEquals(4, Direction.values().length);
        assertNotNull(Direction.valueOf("UP"));
        assertNotNull(Direction.valueOf("DOWN"));
        assertNotNull(Direction.valueOf("LEFT"));
        assertNotNull(Direction.valueOf("RIGHT"));
    }
    
    @Test
    void testEnumCount() {
        assertEquals(4, Direction.values().length);
    }
}
