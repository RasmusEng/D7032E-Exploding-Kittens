package ek.expansion;

import ek.model.Card;
import ek.model.Deck;
import ek.util.Randomizer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class FasterGameTest {

    /** Fisher–Yates with nextInt(bound) = bound - 1 swaps nothing, so the deck keeps its order. */
    private static final Randomizer NO_SHUFFLE = bound -> bound - 1;

    private static Deck deck(int skips, int cats) {
        Deck deck = new Deck();
        for (int i = 0; i < skips; i++) {
            deck.addToBottom(new Card(SKIP));
        }
        for (int i = 0; i < cats; i++) {
            deck.addToBottom(new Card(TACO_CAT));
        }
        return deck;
    }

    @Test
    void afterDeal_removesAThirdOfTheDeck() {
        Deck deck = deck(34, 0);
        new FasterGame().afterDeal(deck, NO_SHUFFLE);
        assertEquals(34 - 11, deck.size());
    }

    @Test
    void afterDeal_shufflesWithTheInjectedRandomizer_soTheRemovedCardsAreRandom() {
        List<Integer> bounds = new ArrayList<>();
        new FasterGame().afterDeal(deck(30, 0), bound -> { bounds.add(bound); return bound - 1; });
        assertEquals(30, bounds.getFirst());
    }

    @Test
    void afterDeal_removesFromTheTopAfterShuffling() {
        // no shuffle: the 10 Skips on top are the third that goes
        Deck deck = deck(10, 20);
        new FasterGame().afterDeal(deck, NO_SHUFFLE);
        assertEquals(0, deck.count(SKIP));
        assertEquals(20, deck.count(TACO_CAT));
    }

    @Test
    void afterDeal_tinyDeck_removesNothing() {
        Deck deck = deck(2, 0);
        new FasterGame().afterDeal(deck, NO_SHUFFLE);
        assertEquals(2, deck.size());
    }

    @Test
    void hasAMenuNameAndDescription() {
        assertFalse(new FasterGame().name().isBlank());
        assertFalse(new FasterGame().description().isBlank());
    }
}
