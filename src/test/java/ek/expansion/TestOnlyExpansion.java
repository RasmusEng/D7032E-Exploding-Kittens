package ek.expansion;

import ek.model.CardType;

import java.util.Map;

/**
 * A fake expansion that exists only on the test classpath. It is registered in
 * {@code src/test/resources/META-INF/services}, which proves a new expansion is found
 * without changing any production class. Public with a public no-arg constructor, as
 * {@link java.util.ServiceLoader} requires.
 */
public final class TestOnlyExpansion implements Expansion {

    @Override
    public String name() {
        return "Test-only expansion";
    }

    @Override
    public String description() {
        return "Adds 3 Test Cards.";
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public Map<CardType, Integer> deckCards(int playerCount) {
        return Map.of(TestCardType.TEST_CARD, 3);
    }
}
