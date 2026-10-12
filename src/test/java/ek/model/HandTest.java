package ek.model;

import org.junit.jupiter.api.Test;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class HandTest {

    @Test
    void addAndCount() {
        Hand hand = new Hand();
        hand.add(new Card(TACO_CAT));
        hand.add(new Card(TACO_CAT));
        hand.add(new Card(DEFUSE));
        assertEquals(3, hand.size());
        assertEquals(2, hand.count(TACO_CAT));
        assertTrue(hand.contains(DEFUSE));
        assertFalse(hand.contains(NOPE));
    }

    @Test
    void remove_returnsCardOfType() {
        Hand hand = new Hand();
        hand.add(new Card(ATTACK));
        hand.add(new Card(DEFUSE));
        assertEquals(DEFUSE, hand.remove(DEFUSE).type());
        assertEquals(1, hand.size());
    }

    @Test
    void remove_missingType_throws() {
        Hand hand = new Hand();
        hand.add(new Card(ATTACK));
        assertThrows(IllegalArgumentException.class, () -> hand.remove(DEFUSE));
        assertEquals(1, hand.size());
    }

    @Test
    void removeAt_removesThatCard() {
        Hand hand = new Hand();
        hand.add(new Card(ATTACK));
        hand.add(new Card(SKIP));
        assertEquals(SKIP, hand.removeAt(1).type());
        assertEquals(1, hand.size());
    }

    @Test
    void discardAll_emptiesHand() {
        Hand hand = new Hand();
        hand.add(new Card(ATTACK));
        hand.add(new Card(SKIP));
        assertEquals(2, hand.discardAll().size());
        assertTrue(hand.isEmpty());
    }

    @Test
    void cards_isUnmodifiable() {
        Hand hand = new Hand();
        assertThrows(UnsupportedOperationException.class, () -> hand.cards().add(new Card(ATTACK)));
    }
}
