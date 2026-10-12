package ek.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** The cards a player holds. There is no minimum or maximum hand size. */
public final class Hand {

    private final List<Card> cards = new ArrayList<>();

    public void add(Card card) {
        cards.add(Objects.requireNonNull(card, "card"));
    }

    /**
     * Removes and returns one card of the given type.
     *
     * @throws IllegalArgumentException if the hand holds no card of that type
     */
    public Card remove(CardType type) {
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).type().equals(type)) {
                return cards.remove(i);
            }
        }
        throw new IllegalArgumentException("Hand has no " + type.displayName());
    }

    /**
     * Removes and returns the card at {@code index}. Used for random steals,
     * where the caller picks the index with an injected randomizer.
     *
     * @throws IndexOutOfBoundsException if {@code index} is outside {@code 0..size()-1}
     */
    public Card removeAt(int index) {
        return cards.remove(index);
    }

    /** Removes and returns every card in the hand, leaving it empty. */
    public List<Card> discardAll() {
        List<Card> discarded = List.copyOf(cards);
        cards.clear();
        return discarded;
    }

    public boolean contains(CardType type) {
        return count(type) > 0;
    }

    public int count(CardType type) {
        return (int) cards.stream().filter(c -> c.type().equals(type)).count();
    }

    /** Read-only view of the cards. */
    public List<Card> cards() {
        return Collections.unmodifiableList(cards);
    }

    public int size() {
        return cards.size();
    }

    public boolean isEmpty() {
        return cards.isEmpty();
    }
}
