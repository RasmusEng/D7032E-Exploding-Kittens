package ek.util;

import java.time.Duration;

/** Waits for a while. Injected where the program deliberately pauses, so tests don't wait. */
@FunctionalInterface
public interface Sleeper {

    /** Doesn't wait at all; for tests and for consoles that shouldn't pause. */
    Sleeper NONE = duration -> {
    };

    /** Really waits. If interrupted, it stops early and keeps the interrupt flag. */
    Sleeper REAL = duration -> {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    };

    void sleep(Duration duration);
}
