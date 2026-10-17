package ek.player;

import ek.model.Card;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class CardChoiceTest {

    private static final List<Card> THREE = List.of(new Card(SKIP), new Card(ATTACK), new Card(NOPE));

    @Test
    void orderOf_picksEveryOption() {
        assertEquals(3, CardChoice.orderOf("Order", THREE).picks());
    }

    @Test
    void accepts_exactlyPicksDistinctIndexesInRange() {
        CardChoice order = CardChoice.orderOf("Order", THREE);
        assertTrue(order.accepts(List.of(2, 0, 1)));
        assertFalse(order.accepts(List.of(0, 0, 1)), "duplicate");
        assertFalse(order.accepts(List.of(0, 1, 3)), "out of range");
        assertFalse(order.accepts(List.of(0, -1, 1)), "negative");
        assertFalse(order.accepts(List.of(0, 1)), "too few");
        assertFalse(order.accepts(Arrays.asList(0, null, 1)), "null index");
        assertFalse(order.accepts(null), "no answer");
    }

    @Test
    void accepts_aSubset() {
        CardChoice one = new CardChoice("Pick one", THREE, 1);
        assertTrue(one.accepts(List.of(2)));
        assertFalse(one.accepts(List.of(2, 1)));
    }

    @Test
    void invalidPicks_rejected() {
        assertThrows(IllegalArgumentException.class, () -> new CardChoice("x", THREE, 0));
        assertThrows(IllegalArgumentException.class, () -> new CardChoice("x", THREE, 4));
        assertThrows(IllegalArgumentException.class, () -> CardChoice.orderOf("x", List.of()));
    }
}