package ek.expansion;

import ek.model.Deck;
import ek.util.Randomizer;

/**
 * A fake variant that exists only on the test classpath, registered in
 * {@code src/test/resources/META-INF/services}. It removes the top card after dealing, so
 * tests can see that the variant hook ran, and when.
 */
public final class TestOnlyVariant implements GameVariant {

    @Override
    public String name() {
        return "Test-only variant";
    }

    @Override
    public String description() {
        return "Removes the top card after dealing.";
    }

    @Override
    public void afterDeal(Deck deck, Randomizer randomizer) {
        deck.draw();
    }
}
