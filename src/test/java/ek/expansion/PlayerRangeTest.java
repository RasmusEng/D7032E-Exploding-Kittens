package ek.expansion;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PlayerRangeTest {

    @Test
    void rule01_baseGame_twoToFive() {
        assertEquals(new PlayerRange(2, 5), PlayerRange.of(List.of(new BaseGame())));
    }

    @Test
    void contains_inclusiveBounds() {
        PlayerRange range = new PlayerRange(2, 5);
        assertFalse(range.contains(1));
        assertTrue(range.contains(2));
        assertTrue(range.contains(5));
        assertFalse(range.contains(6));
    }

    @Test
    void toString_showsTheRange() {
        assertEquals("2–5", new PlayerRange(2, 5).toString());
    }

    @Test
    void invalidBounds_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new PlayerRange(3, 2));
        assertThrows(IllegalArgumentException.class, () -> new PlayerRange(0, 2));
    }

    @Test
    void deckFactory_usesTheSameRange() {
        assertEquals(new PlayerRange(2, 5), new DeckFactory(List.of(new BaseGame())).playerRange());
    }
}
