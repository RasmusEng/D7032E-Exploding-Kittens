package ek.fakeimploding;

import ek.model.CardType;

/** The fake expansion's cards: tiny stand-ins for the Imploding Kittens cards. */
public enum FakeCardType implements CardType {
    FAKE_KITTEN("Fake Imploding Kitten");

    private final String displayName;

    FakeCardType(String displayName) {
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