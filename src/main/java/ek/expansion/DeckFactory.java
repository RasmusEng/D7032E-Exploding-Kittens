package ek.expansion;

import ek.model.Card;
import ek.model.CardType;
import ek.model.Deck;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Builds the deck from the active expansions: the pre-deal deck, and the cards added after dealing. */
public final class DeckFactory {

    private final List<Expansion> expansions;

    public DeckFactory(List<Expansion> expansions) {
        this.expansions = List.copyOf(expansions);
    }

    /** Returns a new deck holding every expansion's cards for {@code playerCount} players. */
    public Deck build(int playerCount) {
        Deck deck = new Deck();
        for (Expansion expansion : expansions) {
            addAll(deck, expansion.deckCards(playerCount));
        }
        return deck;
    }

    /**
     * Rule 6: adds the cards the expansions put in after dealing, at the bottom (the deck is
     * shuffled afterwards).
     *
     * @throws IllegalStateException if the expansions end with a negative count
     */
    public void addAfterDeal(Deck deck, int playerCount) {
        addAll(deck, afterDeal(playerCount));
    }

    /** Every card type the expansions can put into the deck, e.g. to offer when naming a card. */
    public Set<CardType> cardTypes(int playerCount) {
        Set<CardType> types = new LinkedHashSet<>();
        expansions.forEach(e -> types.addAll(e.deckCards(playerCount).keySet()));
        types.addAll(afterDeal(playerCount).keySet());
        return types;
    }

    /** Each expansion adjusts the counts of the ones before it, in order. */
    private Map<CardType, Integer> afterDeal(int playerCount) {
        Map<CardType, Integer> cards = new LinkedHashMap<>();
        for (Expansion expansion : expansions) {
            cards = expansion.cardsAfterDeal(cards, playerCount);
        }
        return cards;
    }

    private static void addAll(Deck deck, Map<CardType, Integer> counts) {
        counts.forEach((type, copies) -> {
            if (copies < 0) {
                throw new IllegalStateException("Negative count for " + type.displayName() + ": " + copies);
            }
            for (int i = 0; i < copies; i++) {
                deck.addToBottom(new Card(type));
            }
        });
    }
}
