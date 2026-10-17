package ek.model;

import ek.util.Randomizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * The draw pile. Index 0 is the top of the deck.
 *
 * <p>Each position holds a card and whether it lies face up (the Imploding Kitten is put
 * back face up). Face-up belongs to the position, not to the {@link Card}, which stays an
 * immutable value; the flag moves with its card through shuffles, inserts and reorders.
 *
 * <p>Randomness is injected into {@link #shuffle(Randomizer)} so tests can be deterministic.
 */
public final class Deck {

    /** An end of the deck a card can be drawn from. */
    public enum Side { TOP, BOTTOM }

    private record Slot(Card card, boolean faceUp) {
    }

    private final List<Slot> slots = new ArrayList<>();

    /** Adds a card face down at the bottom (building the deck). */
    public void addToBottom(Card card) {
        slots.add(new Slot(Objects.requireNonNull(card, "card"), false));
    }

    public void addAll(Collection<Card> newCards) { // never used
        newCards.forEach(this::addToBottom);
    }

    /**
     * Removes and returns the top card.
     * @throws IllegalStateException if the deck is empty
     */
    public Card draw() {
        return draw(Side.TOP);
    }

    /**
     * Removes and returns the bottom card (Draw from the Bottom).
     * @throws IllegalStateException if the deck is empty
     */
    public Card drawBottom() {
        return draw(Side.BOTTOM);
    }

    /**
     * Removes and returns the card at {@code side}.
     * @throws IllegalStateException if the deck is empty
     */
    public Card draw(Side side) {
        requireCards();
        return (side == Side.TOP ? slots.removeFirst() : slots.removeLast()).card();
    }

    /**
     * Whether the card at {@code side}, the next one drawn from there, lies face up.
     * @throws IllegalStateException if the deck is empty
     */
    public boolean isFaceUp(Side side) {
        requireCards();
        return (side == Side.TOP ? slots.getFirst() : slots.getLast()).faceUp();
    }

    /** Positions of the face-up cards, top first (0 = top). Everyone may see these. */
    public List<Integer> faceUpPositions() {
        return IntStream.range(0, slots.size()).filter(i -> slots.get(i).faceUp()).boxed().toList();
    }

    /**
     * Returns up to {@code n} cards from the top without removing them, top first.
     * Returns fewer if the deck holds fewer than {@code n} cards.
     */
    public List<Card> peek(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative: " + n);
        }
        return slots.subList(0, Math.min(n, slots.size())).stream().map(Slot::card).toList();
    }

    /**
     * Inserts a card face down at the given position; 0 is the top and {@link #size()} is the bottom.
     *
     * @throws IndexOutOfBoundsException if {@code index} is outside {@code 0..size()}
     */
    public void insertAt(int index, Card card) {
        insert(index, card, false);
    }

    /**
     * Inserts a card face up, like {@link #insertAt} (an Imploding Kitten put back).
     *
     * @throws IndexOutOfBoundsException if {@code index} is outside {@code 0..size()}
     */
    public void insertFaceUpAt(int index, Card card) {
        insert(index, card, true);
    }

    /**
     * Puts the top {@code order.size()} cards in a new order: the card at position
     * {@code order.get(i)} becomes position {@code i} (Alter the Future).
     *
     * @throws IllegalArgumentException if {@code order} is not a reordering of {@code 0..order.size()-1}
     *                                  within the deck; the deck is then unchanged
     */
    public void reorderTop(List<Integer> order) {
        int n = order.size();
        if (n > slots.size() || !order.stream().sorted().toList().equals(IntStream.range(0, n).boxed().toList())) {
            throw new IllegalArgumentException("Not a reordering of the top " + n + " cards: " + order);
        }
        List<Slot> top = List.copyOf(slots.subList(0, n));
        for (int i = 0; i < n; i++) {
            slots.set(i, top.get(order.get(i)));
        }
    }

    /** Shuffles the deck uniformly (Fisher–Yates) using the given randomizer. */
    public void shuffle(Randomizer randomizer) {
        for (int i = slots.size() - 1; i > 0; i--) {
            Collections.swap(slots, i, randomizer.nextInt(i + 1));
        }
    }

    /** Number of cards of the given type in the deck. */
    public int count(CardType type) {
        return (int) slots.stream().filter(s -> s.card().type().equals(type)).count();
    }

    public int size() {
        return slots.size();
    }

    public boolean isEmpty() {
        return slots.isEmpty();
    }

    private void insert(int index, Card card, boolean faceUp) {
        Objects.requireNonNull(card, "card");
        if (index < 0 || index > slots.size()) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " outside 0.." + slots.size());
        }
        slots.add(index, new Slot(card, faceUp));
    }

    private void requireCards() {
        if (slots.isEmpty()) {
            throw new IllegalStateException("The deck is empty");
        }
    }
}