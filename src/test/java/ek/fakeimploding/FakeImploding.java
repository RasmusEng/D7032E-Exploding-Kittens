package ek.fakeimploding;

import ek.expansion.Expansion;
import ek.model.CardType;

import java.util.LinkedHashMap;
import java.util.Map;

import static ek.fakeimploding.FakeCardType.FAKE_KITTEN;
import static ek.model.BaseCardType.DEFUSE;
import static ek.model.BaseCardType.EXPLODING_KITTEN;

/**
 * A test-only stand-in for Imploding Kittens, in its own package and registered with one line
 * in {@code src/test/resources/META-INF/services}. It uses only the extension points, so it
 * proves Imploding Kittens can be added without changing production code.
 */
public final class FakeImploding implements Expansion {

    private static final int MAX_PLAYERS = 6;

    @Override
    public String name() {
        return "Fake Imploding";
    }

    @Override
    public String description() {
        return "Test stand-in for Imploding Kittens.";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public int maxPlayers() {
        return MAX_PLAYERS;
    }

    @Override
    public Map<CardType, Integer> deckCards(int playerCount) {
        return Map.of();
    }

    /** With 6 players every Defuse is dealt, so none go into the deck. */
    @Override
    public Map<CardType, Integer> deckCards(Map<CardType, Integer> soFar, int playerCount) {
        Map<CardType, Integer> cards = new LinkedHashMap<>(Expansion.super.deckCards(soFar, playerCount));
        if (playerCount == MAX_PLAYERS) {
            cards.put(DEFUSE, 0);
        }
        return cards;
    }

    /** Like the Imploding Kitten, its kitten replaces one Exploding Kitten. */
    @Override
    public Map<CardType, Integer> cardsAfterDeal(Map<CardType, Integer> soFar, int playerCount) {
        Map<CardType, Integer> cards = new LinkedHashMap<>(soFar);
        cards.merge(EXPLODING_KITTEN, -1, Integer::sum);
        cards.put(FAKE_KITTEN, 1);
        return cards;
    }
}