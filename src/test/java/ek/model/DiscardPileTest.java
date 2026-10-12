package ek.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class DiscardPileTest {

    @Test
    void addAndAddAll() {
        DiscardPile pile = new DiscardPile();
        pile.add(new Card(NOPE));
        pile.addAll(List.of(new Card(ATTACK), new Card(SKIP)));
        assertEquals(3, pile.size());
        assertEquals(NOPE, pile.cards().get(0).type());
    }

    @Test
    void cards_isUnmodifiable() {
        DiscardPile pile = new DiscardPile();
        assertThrows(UnsupportedOperationException.class, () -> pile.cards().add(new Card(ATTACK)));
    }
}
