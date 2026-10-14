package ek.expansion;

import ek.model.CardType;

/** Card types that only the test expansions put into the deck. */
public enum TestCardType implements CardType {
    TEST_CARD("Test Card"),
    OTHER_TEST_CARD("Other Test Card");

    private final String displayName;

    TestCardType(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String description() {
        return "Only used in tests.";
    }
}
