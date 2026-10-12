package ek.util;

import java.util.Random;

/** {@link Randomizer} backed by {@link java.util.Random}. */
public final class JavaRandomizer implements Randomizer {

    private final Random random;

    public JavaRandomizer() {
        this(new Random());
    }

    public JavaRandomizer(Random random) {
        this.random = random;
    }

    @Override
    public int nextInt(int bound) {
        return random.nextInt(bound);
    }
}
