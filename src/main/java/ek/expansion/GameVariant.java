package ek.expansion;

import ek.model.Deck;
import ek.util.Randomizer;

/**
 * A variant of the rules, such as the official "Faster game" (none is implemented yet). It
 * may change the deck after the hands are dealt and before the Exploding Kittens go in; that
 * is its only hook, so a new variant is a new class and leaves {@code GameSetup} unchanged.
 */
public interface GameVariant extends GameOption {

    /**
     * Changes the draw deck after dealing, before the kittens are added (rule 6).
     *
     * @param deck       the deck left after dealing
     * @param randomizer the game's source of randomness
     */
    void afterDeal(Deck deck, Randomizer randomizer);
}
