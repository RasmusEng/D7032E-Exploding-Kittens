package ek.util;

/**
 * Source of randomness, injected wherever the game needs chance
 * (shuffling, start player, random steals) so tests can be deterministic.
 */
@FunctionalInterface
public interface Randomizer {

    /** Returns a value in {@code 0..bound-1}. */
    int nextInt(int bound);
}
