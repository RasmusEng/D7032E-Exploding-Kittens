package ek.expansion;

import java.util.List;

/**
 * How many players (humans and bots together) a game can have.
 *
 * @param min fewest players, at least 1
 * @param max most players, at least {@code min}
 */
public record PlayerRange(int min, int max) {

    /** Rule 1: the base game has 2–5 players. */
    static final int BASE_GAME_MIN = 2;
    static final int BASE_GAME_MAX = 5;

    public PlayerRange {
        if (min < 1 || max < min) {
            throw new IllegalArgumentException("Invalid player range " + min + "–" + max);
        }
    }

    /**
     * The range for a game with these expansions, and the only place it is worked out:
     * at least 2 players, and at most the largest {@link Expansion#maxPlayers()} (rule 1:
     * 2–5 for the base game; Imploding Kittens allows 6).
     */
    public static PlayerRange of(List<Expansion> expansions) {
        int max = expansions.stream().mapToInt(Expansion::maxPlayers).max().orElse(BASE_GAME_MAX);
        return new PlayerRange(BASE_GAME_MIN, max);
    }

    public boolean contains(int players) {
        return players >= min && players <= max;
    }

    @Override
    public String toString() {
        return min + "–" + max;
    }
}
