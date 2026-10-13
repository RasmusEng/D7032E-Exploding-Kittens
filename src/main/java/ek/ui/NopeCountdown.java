package ek.ui;

import java.time.Duration;

/**
 * The text of the Nope prompt as its window runs out: "Nope Bob's Attack? Press Enter ■■■■□ 4s".
 * Pure text: the {@link ConsoleController} decides when to redraw it.
 */
final class NopeCountdown {

    private static final int MAX_BLOCKS = 10;

    private final String question;
    private final long totalSeconds;

    /** @param question e.g. "Nope Bob's Attack?" */
    NopeCountdown(String question, Duration window) {
        this.question = question;
        this.totalSeconds = secondsLeft(window, Duration.ZERO);
    }

    /** Whole seconds left, rounded up so the last second shows "1s", never below 0. */
    static long secondsLeft(Duration window, Duration elapsed) {
        Duration left = window.minus(elapsed);
        return left.isNegative() ? 0 : (left.toMillis() + 999) / 1000;
    }

    /** The prompt for a terminal that can't redraw it: only the window's length. */
    String fixed() {
        return question + " [Enter to Nope, " + totalSeconds + "s] ";
    }

    /** The prompt with a bar and the seconds left, for a terminal that redraws it. */
    String live(long secondsLeft) {
        long blocks = Math.min(totalSeconds, MAX_BLOCKS);
        long filled = totalSeconds == 0 ? 0 : Math.round((double) secondsLeft * blocks / totalSeconds);
        return question + " Press Enter " + "■".repeat((int) filled) + "□".repeat((int) (blocks - filled))
                + " " + secondsLeft + "s ";
    }
}
