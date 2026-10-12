package ek.player;

import ek.model.CardType;

import java.time.Duration;
import java.util.List;

/**
 * A chance to play Nope on cards just played. Valid only while the Nope window is open.
 *
 * <p>Thread-safe: {@link #playNope()} may be called from any thread, e.g. an input or
 * network thread, at any time; late calls are refused.
 */
public interface NopeOffer {

    /** Name of the player whose cards would be stopped. */
    String playedBy();

    /** The cards played (the original play, not the Nopes on it). */
    List<CardType> cards();

    /** Nopes already played on these cards. If odd, your Nope works as a Yup and lets the play happen. */
    int nopesSoFar();

    /**
     * How long the window is open in total, counted from when the offer was made. Only for
     * showing a countdown: {@link #isOpen()} is what decides.
     */
    Duration window();

    /** Whether the window is still open; a controller asking a human can stop waiting when it closes. */
    boolean isOpen();

    /**
     * Plays one of your Nope cards.
     *
     * @return {@code true} if accepted; {@code false} if the window has closed
     *         or another player was first
     */
    boolean playNope();
}
