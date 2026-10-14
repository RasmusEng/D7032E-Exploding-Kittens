package ek.expansion;

import ek.model.Deck;
import ek.util.Randomizer;

/**
 * The official "Faster game" variant: after dealing, about a third of the deck is removed at
 * random, unseen, before the Exploding Kittens go in. The kittens are added afterwards as
 * usual (rule 6), so the game is shorter but just as deadly.
 */
public final class FasterGame implements GameVariant {

    @Override
    public String name() {
        return "Faster game";
    }

    @Override
    public String description() {
        return "Removes a third of the deck at random before the Exploding Kittens go in.";
    }

    /** Shuffles, so the removed cards are random whatever the deck's order, then drops the top third. */
    @Override
    public void afterDeal(Deck deck, Randomizer randomizer) {
        deck.shuffle(randomizer);
        int remove = deck.size() / 3;
        for (int i = 0; i < remove; i++) {
            deck.draw();
        }
    }
}
