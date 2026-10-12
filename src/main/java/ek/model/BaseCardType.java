package ek.model;

/** Card types that are in the base Exploding Kittens game. */
public enum BaseCardType implements CardType {
    EXPLODING_KITTEN("Exploding Kitten", "Drawing it makes you explode unless you play a Defuse."),
    DEFUSE("Defuse", "Saves you from an Exploding Kitten; you put the kitten back anywhere in the deck."),
    ATTACK("Attack", "End your turn without drawing; the next player takes 2 turns (Attacks stack)."),
    FAVOR("Favor", "Another player gives you a card of their choice."),
    NOPE("Nope", "Stop another player's action; play it on a Nope to Yup. Any time, within 5 seconds."),
    SHUFFLE("Shuffle", "Shuffle the deck."),
    SKIP("Skip", "End one turn without drawing."),
    SEE_THE_FUTURE("See The Future", "Privately look at the top 3 cards of the deck."),
    HAIRY_POTATO_CAT("Hairy Potato Cat", CatText.TEXT),
    CATTERMELON("Cattermelon", CatText.TEXT),
    RAINBOW_RALPHING_CAT("Rainbow Ralphing Cat", CatText.TEXT),
    TACO_CAT("Taco Cat", CatText.TEXT),
    OVERWEIGHT_BIKINI_CAT("Overweight Bikini Cat", CatText.TEXT);

    private final String displayName;
    private final String description;

    BaseCardType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @Override
    public String displayName() {
        return displayName;
    }

    @Override
    public String description() {
        return description;
    }

    /** Enum constants can't refer to the enum's own static fields, so the shared text lives here. */
    private static final class CatText {
        static final String TEXT = "No effect alone. Pair: steal a random card. Three: name a card to take.";
    }
}
