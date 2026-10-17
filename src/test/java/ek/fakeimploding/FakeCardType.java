package ek.fakeimploding;

import ek.model.CardType;

/** The fake expansion's cards: tiny stand-ins for the Imploding Kittens cards. */
public enum FakeCardType implements CardType {
    FAKE_KITTEN("Fake Imploding Kitten"),
    FAKE_ALTER_THE_FUTURE("Fake Alter the Future"),
    FAKE_REVERSE("Fake Reverse"),
    FAKE_TARGETED_ATTACK("Fake Targeted Attack");

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