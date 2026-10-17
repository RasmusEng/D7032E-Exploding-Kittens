package ek.model;

import java.io.Serializable;

/**
 * The kinda card (e.g. Attack, Defuse).
 * <p>An interface rather than a single enum so that expansions can contribute their own
 * card types (as their own enums) without modifying the base game (Open/Closed).
 */
public interface CardType extends Serializable {

    /** Human-readable card name only used for views not for the actual game logic. */
    String displayName();

    /** One-line rules text shown as help in views; like {@link #displayName()}, never used by game logic. */
    String description();

    /**
     * Whether a play with this card can be Noped (rule 12). Every base card can; an
     * expansion's card can say no (the Imploding Kitten), and a play with such a card skips
     * the Nope window. Draws never open one.
     */
    default boolean canBeNoped() {
        return true;
    }

    /** Whether this is a Cat Card (the five base cats; an expansion's cats too). No effect on its own. */
    default boolean isCat() {
        return false;
    }

    /**
     * Whether this card may stand in for {@code other} in a combo (rules 11f, g), as a Feral
     * Cat counts as any Cat Card ({@code other.isCat()}). No base card is a wildcard.
     */
    default boolean substitutesFor(CardType other) {
        return false;
    }
}
