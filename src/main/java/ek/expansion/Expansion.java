package ek.expansion;

import ek.engine.CardEffect;
import ek.engine.DrawEffect;
import ek.model.CardType;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A set of cards that can be put into the deck, with what they do when played or drawn.
 * The base game is one expansion; Imploding Kittens etc. would be others, added without
 * changing existing classes: an optional expansion is found on the classpath through
 * {@code META-INF/services/ek.expansion.Expansion} (see {@link ExpansionRegistry}), and
 * {@link #order()} decides where it is applied.
 *
 * <p>An expansion and its card types must live in an {@code ek.} package (e.g.
 * {@code ek.imploding}): the network only accepts {@code ek.} classes, and card types are
 * sent to the clients.
 */
@FunctionalInterface
public interface Expansion extends GameOption {

    /**
     * Cards this expansion puts into the deck before the first hands are dealt.
     *
     * @param playerCount number of players in the game (some counts depend on it)
     * @return card type to number of copies, in a stable iteration order
     */
    Map<CardType, Integer> deckCards(int playerCount);

    /**
     * The deck before dealing once this expansion is added. Expansions are asked in order,
     * each getting the counts so far (like {@link #cardsAfterDeal}), so an expansion can
     * also change another's counts (Imploding Kittens puts no Defuse in a 6-player deck).
     *
     * @param soFar       card type to copies, from the expansions before this one
     * @param playerCount number of players in the game
     * @return the new counts; the default adds {@link #deckCards(int)} to {@code soFar}
     */
    default Map<CardType, Integer> deckCards(Map<CardType, Integer> soFar, int playerCount) {
        Map<CardType, Integer> cards = new LinkedHashMap<>(soFar);
        deckCards(playerCount).forEach((type, copies) -> cards.merge(type, copies, Integer::sum));
        return cards;
    }

    /**
     * Most players a game with this expansion can have. A game allows the largest among its
     * expansions (see {@link PlayerRange#of}); the default is the base game's limit.
     */
    default int maxPlayers() {
        return PlayerRange.BASE_GAME_MAX;
    }

    /** Effects of the card types this expansion adds that can be played on their own. */
    default Map<CardType, CardEffect> cardEffects() {
        return Map.of();
    }

    /**
     * Cards put into the deck after the hands are dealt (rule 6), e.g. the kittens.
     * Expansions are asked in order, each getting the counts so far, so an expansion can
     * also change another's counts (Imploding Kittens replaces one Exploding Kitten).
     *
     * @param soFar       card type to copies, from the expansions before this one
     * @param playerCount number of players in the game
     * @return the new counts; the default leaves {@code soFar} unchanged
     */
    default Map<CardType, Integer> cardsAfterDeal(Map<CardType, Integer> soFar, int playerCount) {
        return soFar;
    }

    /** What drawing each of this expansion's special cards does; other cards go into the hand. */
    default Map<CardType, DrawEffect> drawEffects() {
        return Map.of();
    }
}
