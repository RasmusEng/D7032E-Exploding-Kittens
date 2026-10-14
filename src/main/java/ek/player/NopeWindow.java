package ek.player;

import ek.model.CardType;

import java.time.Duration;
import java.util.List;

/**
 * A Nope window as everyone sees it (rule 12): which play may be stopped, and for how long.
 * It always runs its full length; only a Nope ends it early, and that opens a new one.
 *
 * <p>Thread-safe: {@link #isOpen()} may be called from any thread.
 */
public interface NopeWindow {

    /** Name of the player whose cards would be stopped. */
    String playedBy();

    /** The cards played (the original play, not the Nopes on it). */
    List<CardType> cards();

    /** Nopes already played on these cards. If odd, a Nope works as a Yup and lets the play happen. */
    int nopesSoFar();

    /**
     * How long the window is open in total, counted from when it was opened. Only for
     * showing a countdown: {@link #isOpen()} is what decides.
     */
    Duration window();

    /** Whether the window is still open; a countdown stops when it closes. */
    boolean isOpen();
}
