package ek.expansion;

import ek.engine.CardEffect;
import ek.model.CardType;

import java.util.Map;

/**
 * A set of cards that can be put into the deck. The base game is one expansion;
 * Imploding Kittens etc. would be others, added without changing existing classes.
 */
@FunctionalInterface
public interface Expansion {

    /**
     * Cards this expansion puts into the deck before the first hands are dealt.
     *
     * @param playerCount number of players in the game (some counts depend on it)
     * @return card type to number of copies, in a stable iteration order
     */
    Map<CardType, Integer> deckCards(int playerCount);

    /** Effects of the card types this expansion adds that can be played on their own. */
    default Map<CardType, CardEffect> cardEffects() {
        return Map.of();
    }
}
