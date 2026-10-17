package ek.model;

/** Card types that are in the base Exploding Kittens game. */
public enum BaseCardType implements CardType {
    EXPLODING_KITTEN("Exploding Kitten", "Drawing it makes you explode unless you play a Defuse.", false),
    DEFUSE("Defuse", "Saves you from an Exploding Kitten; you put the kitten back anywhere in the deck.", false),
    ATTACK("Attack", "End your turn without drawing; the next player takes 2 turns (Attacks stack).", false),
    FAVOR("Favor", "Another player gives you a card of their choice.", false),
    NOPE("Nope", "Stop another player's action; play it on a Nope to Yup. Any time, within 5 seconds.", false),
    SHUFFLE("Shuffle", "Shuffle the deck.", false),
    SKIP("Skip", "End one turn without drawing.", false),
    SEE_THE_FUTURE("See The Future", "Privately look at the top 3 cards of the deck.", false),
    HAIRY_POTATO_CAT("Hairy Potato Cat", CatText.TEXT, true),
    CATTERMELON("Cattermelon", CatText.TEXT, true),
    RAINBOW_RALPHING_CAT("Rainbow Ralphing Cat", CatText.TEXT, true),
    TACO_CAT("Taco Cat", CatText.TEXT, true),
    OVERWEIGHT_BIKINI_CAT("Overweight Bikini Cat", CatText.TEXT, true);

    private final String displayName;
    private final String description;
    private final boolean cat;

    BaseCardType(String displayName, String description, boolean cat) {
        this.displayName = displayName;
        this.description = description;
        this.cat = cat;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String description() {
        return description;
    }

    /** The five Cat Cards: no effect alone, only in combos. */
    @Override
    public boolean isCat() {
        return cat;
    }

    /** Enum constants can't refer to the enum's own static fields, so the shared text lives here. */
    private static final class CatText {
        static final String TEXT = "No effect alone. Pair: steal a random card. Three: name a card to take.";
    }
}
