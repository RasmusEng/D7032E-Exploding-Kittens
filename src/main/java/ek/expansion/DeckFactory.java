package ek.expansion;

import ek.model.Card;
import ek.model.Deck;

import java.util.List;

/** Builds the unshuffled pre-deal deck from the active expansions. */
public final class DeckFactory {

    private final List<Expansion> expansions;

    public DeckFactory(List<Expansion> expansions) {
        this.expansions = List.copyOf(expansions);
    }

    /** Returns a new deck holding every expansion's cards for {@code playerCount} players. */
    public Deck build(int playerCount) {
        Deck deck = new Deck();
        for (Expansion expansion : expansions) {
            expansion.deckCards(playerCount).forEach((type, copies) -> {
                for (int i = 0; i < copies; i++) {
                    deck.addToBottom(new Card(type));
                }
            });
        }
        return deck;
    }
}
