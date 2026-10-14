package ek.expansion;

import ek.model.CardType;

import java.util.Map;

/** A registered expansion that can't be created, to check the registry skips and reports it. */
public final class BrokenExpansion implements Expansion {

    public BrokenExpansion() {
        throw new IllegalStateException("broken on purpose");
    }

    @Override
    public Map<CardType, Integer> deckCards(int playerCount) {
        return Map.of();
    }
}
