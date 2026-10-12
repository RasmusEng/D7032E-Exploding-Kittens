package ek.model;

import java.io.Serializable;
import java.util.Objects;

/** A single immutable card. Two cards of the same type are interchangeable. */
public record Card(CardType type) implements Serializable {

    public Card {
        Objects.requireNonNull(type, "type");
    }

    @Override
    public String toString() {
        return type.displayName();
    }
}
