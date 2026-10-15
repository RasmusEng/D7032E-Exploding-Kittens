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
}
