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
    void drawBottom_returnsBottomCard() {
        Deck deck = deckOf(ATTACK, SKIP, NOPE);
        assertEquals(NOPE, deck.drawBottom().type());
        assertEquals(List.of(new Card(ATTACK), new Card(SKIP)), deck.peek(3));
    }

    @Test
    void drawFromASide_sameAsTheNamedMethods() {
        Deck deck = deckOf(ATTACK, SKIP, NOPE);
        assertEquals(ATTACK, deck.draw(Deck.Side.TOP).type());
        assertEquals(NOPE, deck.draw(Deck.Side.BOTTOM).type());
    }

    @Test
    void drawBottom_emptyDeck_throws() {
        assertThrows(IllegalStateException.class, () -> new Deck().drawBottom());
    }

    @Test
    void cards_faceDownByDefault() {
        Deck deck = deckOf(ATTACK, SKIP);
        deck.insertAt(1, new Card(NOPE));
        assertEquals(List.of(), deck.faceUpPositions());
        assertFalse(deck.isFaceUp(Deck.Side.TOP));
    }

    @Test
    void insertFaceUpAt_positionIsPublic() {
        Deck deck = deckOf(ATTACK, SKIP, NOPE);
        deck.insertFaceUpAt(2, new Card(EXPLODING_KITTEN));
        assertEquals(List.of(2), deck.faceUpPositions());
        assertEquals(EXPLODING_KITTEN, deck.peek(3).get(2).type(), "still a normal card");
    }

    @Test
    void faceUpCard_survivesInsertAbove() {
        Deck deck = deckOf(ATTACK, SKIP);
        deck.insertFaceUpAt(1, new Card(EXPLODING_KITTEN));
        deck.insertAt(0, new Card(NOPE));
        assertEquals(List.of(2), deck.faceUpPositions());
    }

    @Test
    void faceUpCard_survivesShuffle() {
        Deck deck = deckOf(ATTACK, SKIP, NOPE, FAVOR);
        deck.insertFaceUpAt(0, new Card(EXPLODING_KITTEN));
        deck.shuffle(bound -> bound - 1 == 4 ? 0 : bound - 1);    // swaps only the top and bottom cards
        assertEquals(List.of(4), deck.faceUpPositions());
        assertEquals(EXPLODING_KITTEN, deck.drawBottom().type());
    }

    @Test
    void faceUpCard_survivesReorder() {
        Deck deck = deckOf(ATTACK, SKIP);
        deck.insertFaceUpAt(0, new Card(EXPLODING_KITTEN));
        deck.reorderTop(List.of(1, 2, 0));
        assertEquals(List.of(2), deck.faceUpPositions());
        assertTrue(deck.isFaceUp(Deck.Side.BOTTOM));
    }

    @Test
    void isFaceUp_tellsTheCardAboutToBeDrawn() {
        Deck deck = deckOf(ATTACK);
        deck.insertFaceUpAt(0, new Card(EXPLODING_KITTEN));
        assertTrue(deck.isFaceUp(Deck.Side.TOP));
        assertFalse(deck.isFaceUp(Deck.Side.BOTTOM));
        deck.draw();
        assertEquals(List.of(), deck.faceUpPositions(), "the face-up card left with the draw");
    }

    @Test
    void isFaceUp_emptyDeck_throws() {
        assertThrows(IllegalStateException.class, () -> new Deck().isFaceUp(Deck.Side.TOP));
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
