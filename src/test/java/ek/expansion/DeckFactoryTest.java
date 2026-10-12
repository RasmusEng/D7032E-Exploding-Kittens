package ek.expansion;

import ek.model.Deck;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class DeckFactoryTest {

    @Test
    void build_baseGame_twoPlayers_has48Cards() {
        Deck deck = new DeckFactory(List.of(new BaseGame())).build(2);
        assertEquals(48, deck.size());
        assertEquals(2, deck.count(DEFUSE));
    }

    @Test
    void build_combinesAllExpansions() {
        Expansion extra = players -> Map.of(NOPE, 3);
        Deck deck = new DeckFactory(List.of(new BaseGame(), extra)).build(2);
        assertEquals(51, deck.size());
        assertEquals(8, deck.count(NOPE));
    }

    @Test
    void build_passesPlayerCountToExpansions() {
        Expansion perPlayer = players -> Map.of(TACO_CAT, players);
        assertEquals(4, new DeckFactory(List.of(perPlayer)).build(4).size());
    }

    @Test
    void build_noExpansions_emptyDeck() {
        assertTrue(new DeckFactory(List.of()).build(3).isEmpty());
    }
}
