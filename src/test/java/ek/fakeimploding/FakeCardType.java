package ek.fakeimploding;

import ek.model.CardType;

/** The fake expansion's cards: tiny stand-ins for the Imploding Kittens cards. */
public enum FakeCardType implements CardType {
    FAKE_KITTEN("Fake Imploding Kitten", false),
    FAKE_ALTER_THE_FUTURE("Fake Alter the Future", true),
    FAKE_REVERSE("Fake Reverse", true),
    FAKE_TARGETED_ATTACK("Fake Targeted Attack", true),
    FAKE_DRAW_FROM_THE_BOTTOM("Fake Draw from the Bottom", true),
    /** Ends the turn without drawing, and can't be Noped. */
    FAKE_UNSTOPPABLE("Fake Unstoppable", false),
    /** Like Feral Cat: a cat that counts as any cat in a combo; no effect alone. */
    FAKE_FERAL_CAT("Fake Feral Cat", true) {
        @Override
        public boolean isCat() {
            return true;
        }

        @Override
        public boolean substitutesFor(CardType other) {
            return other.isCat();
        }
    };

    private final String displayName;
    private final boolean canBeNoped;

    FakeCardType(String displayName, boolean canBeNoped) {
        this.displayName = displayName;
        this.canBeNoped = canBeNoped;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String description() {
        return "Only used in tests.";
    }

    @Override
    public boolean canBeNoped() {
        return canBeNoped;
    }
}