package ek.expansion;

import java.util.List;

/**
 * How many players (humans and bots together) a game can have.
 *
 * @param min fewest players, at least 1
 * @param max most players, at least {@code min}
 */
public record PlayerRange(int min, int max) {

    public PlayerRange {
        if (min < 1 || max < min) {
            throw new IllegalArgumentException("Invalid player range " + min + "–" + max);
        }
    }

    /**
     * The range for a game with these expansions. This is the only place the limit is
     * defined (rule 1: 2–5 for the base game), so an expansion that allows more players
     * changes only this method.
     */
    public static PlayerRange of(List<Expansion> expansions) {
        return new PlayerRange(2, 5);
    }

    public boolean contains(int players) {
        return players >= min && players <= max;
    }

    @Override
    public String toString() {
        return min + "–" + max;
    }
}
