package ek.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Played and discarded cards. Noped cards stay here too. */
public final class DiscardPile {

    private final List<Card> cards = new ArrayList<>();

    public void add(Card card) {
        cards.add(Objects.requireNonNull(card, "card"));
    }

    public void addAll(Collection<Card> newCards) {
        newCards.forEach(this::add);
    }

    /** Read-only view, oldest card first. */
    public List<Card> cards() {
        return Collections.unmodifiableList(cards);
    }

    public int size() {
        return cards.size();
    }
}
