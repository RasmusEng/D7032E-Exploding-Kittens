package ek.model;

import java.util.List;
import java.util.Objects;

/**
 * A seat in the game: a name, a hand and whether the player is still alive.
 *
 * <p>Holds no input/output; decisions are made by a {@code PlayerController}.
 */
public final class Player {

    private final String name;
    private final Hand hand = new Hand();
    private boolean alive = true;

    public Player(String name) {
        this.name = Objects.requireNonNull(name, "name");
    }

    public String name() {
        return name;
    }

    public Hand hand() {
        return hand;
    }

    public boolean isAlive() {
        return alive;
    }

    /**
     * Marks the player as exploded and empties their hand.
     *
     * @return the discarded cards, for the caller to put on the discard pile
     */
    public List<Card> explode() {
        alive = false;
        return hand.discardAll();
    }

    @Override
    public String toString() {
        return name;
    }
}
