package ek.util;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Waits for a signal or until a timeout. Injected where the game waits a fixed time unless
 * something happens (the Nope window), so tests can let the time run out at once and check
 * how long was asked for.
 */
@FunctionalInterface
public interface Waiter {

    /** Really waits. */
    Waiter REAL = (signal, timeout) -> signal.await(timeout.toNanos(), TimeUnit.NANOSECONDS);

    /**
     * Waits until {@code signal} reaches zero or {@code timeout} has passed.
     *
     * @return {@code true} if signalled, {@code false} if the time ran out
     */
    boolean await(CountDownLatch signal, Duration timeout) throws InterruptedException;
}
