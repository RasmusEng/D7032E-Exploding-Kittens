package ek.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    private static Deck deckOf(CardType... types) {
        Deck deck = new Deck();
        for (CardType t : types) {
            deck.addToBottom(new Card(t));
        }
        return deck;
    }

    @Test
    void draw_returnsTopCard() {
        Deck deck = deckOf(ATTACK, SKIP);
        assertEquals(ATTACK, deck.draw().type());
        assertEquals(1, deck.size());
    }

    @Test
    void draw_emptyDeck_throws() {
        assertThrows(IllegalStateException.class, () -> new Deck().draw());
    }

    @Test
    void reorderTop_newTopIsTheChosenCards() {
        Deck deck = deckOf(SKIP, ATTACK, NOPE, TACO_CAT);
        deck.reorderTop(List.of(2, 0, 1));
        assertEquals(List.of(new Card(NOPE), new Card(SKIP), new Card(ATTACK), new Card(TACO_CAT)), deck.peek(4));
    }

    @Test
    void reorderTop_notAPermutation_throwsAndKeepsTheDeck() {
        Deck deck = deckOf(SKIP, ATTACK, NOPE);
        assertThrows(IllegalArgumentException.class, () -> deck.reorderTop(List.of(0, 0, 1)));
        assertThrows(IllegalArgumentException.class, () -> deck.reorderTop(List.of(1, 2)));
        assertThrows(IllegalArgumentException.class, () -> deck.reorderTop(List.of(0, 1, 2, 3)));
        assertEquals(List.of(new Card(SKIP), new Card(ATTACK), new Card(NOPE)), deck.peek(3));
    }

    @Test
    void peek_returnsTopCardsWithoutRemoving() {
        Deck deck = deckOf(ATTACK, SKIP, FAVOR, NOPE, SHUFFLE);
        assertEquals(List.of(new Card(ATTACK), new Card(SKIP), new Card(FAVOR)), deck.peek(3));
        assertEquals(5, deck.size());
    }

    @Test
    void peek_fewerCardsThanRequested_returnsWhatIsThere() {
        assertEquals(2, deckOf(ATTACK, SKIP).peek(3).size());
        assertTrue(new Deck().peek(3).isEmpty());
    }

    @Test
    void insertAt_zero_putsCardOnTop() {
        Deck deck = deckOf(ATTACK, SKIP);
        deck.insertAt(0, new Card(EXPLODING_KITTEN));
        assertEquals(EXPLODING_KITTEN, deck.draw().type());
    }

    @Test
    void insertAt_size_putsCardOnBottom() {
        Deck deck = deckOf(ATTACK, SKIP);
        deck.insertAt(2, new Card(EXPLODING_KITTEN));
        assertEquals(List.of(new Card(ATTACK), new Card(SKIP), new Card(EXPLODING_KITTEN)), deck.peek(3));
    }

    @Test
    void insertAt_middle() {
        Deck deck = deckOf(ATTACK, SKIP);
        deck.insertAt(1, new Card(EXPLODING_KITTEN));
        assertEquals(EXPLODING_KITTEN, deck.peek(2).get(1).type());
    }

    @Test
    void insertAt_outOfRange_throws() {
        Deck deck = deckOf(ATTACK, SKIP);
        Card kitten = new Card(EXPLODING_KITTEN);
        assertThrows(IndexOutOfBoundsException.class, () -> deck.insertAt(-1, kitten));
        assertThrows(IndexOutOfBoundsException.class, () -> deck.insertAt(3, kitten));
    }

    @Test
    void shuffle_usesInjectedRandomizer() {
        Deck deck = deckOf(ATTACK, SKIP, FAVOR);
        // Always picking index 0: i=2 swaps 2<->0 -> [FAVOR, SKIP, ATTACK]; i=1 swaps 1<->0 -> [SKIP, FAVOR, ATTACK]
        deck.shuffle(bound -> 0);
        assertEquals(List.of(new Card(SKIP), new Card(FAVOR), new Card(ATTACK)), deck.peek(3));
    }

    @Test
    void shuffle_keepsAllCards() {
        Deck deck = deckOf(ATTACK, ATTACK, SKIP, FAVOR);
        deck.shuffle(bound -> bound - 1);
        assertEquals(4, deck.size());
        assertEquals(2, deck.count(ATTACK));
    }
}
