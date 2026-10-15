package ek.model;

import ek.util.Randomizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * The draw pile. Index 0 is the top of the deck.
 *
 * <p>Randomness is injected into {@link #shuffle(Randomizer)} so tests can be deterministic.
 */
public final class Deck {

    private final List<Card> cards = new ArrayList<>();
    public void addToBottom(Card card) {
        cards.add(Objects.requireNonNull(card, "card"));
    }

    public void addAll(Collection<Card> newCards) { // never used
        newCards.forEach(this::addToBottom);
    }

    /**
     * Removes and returns the top card.
     * @throws IllegalStateException if the deck is empty
     */
    public Card draw() {
        if (cards.isEmpty()) {
            throw new IllegalStateException("Cannot draw from an empty deck");
        }
        return cards.removeFirst();
    }

    /**
     * Returns up to {@code n} cards from the top without removing them, top first.
     * Returns fewer if the deck holds fewer than {@code n} cards.
     */
    public List<Card> peek(int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be non-negative: " + n);
        }
        return List.copyOf(cards.subList(0, Math.min(n, cards.size())));
    }

    /**
     * Inserts a card at the given position; 0 is the top and {@link #size()} is the bottom.
     *
     * @throws IndexOutOfBoundsException if {@code index} is outside {@code 0..size()}
     */
    public void insertAt(int index, Card card) {
        Objects.requireNonNull(card, "card");
        if (index < 0 || index > cards.size()) {
            throw new IndexOutOfBoundsException(
                    "Index " + index + " outside 0.." + cards.size());
        }
        cards.add(index, card);
    }

    /** Shuffles the deck uniformly (Fisher–Yates) using the given randomizer. */
    public void shuffle(Randomizer randomizer) {
        for (int i = cards.size() - 1; i > 0; i--) {
            Collections.swap(cards, i, randomizer.nextInt(i + 1));
        }
    }

    /** Number of cards of the given type in the deck. */
    public int count(CardType type) {
        return (int) cards.stream().filter(c -> c.type().equals(type)).count();
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
