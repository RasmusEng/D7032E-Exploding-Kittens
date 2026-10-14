package ek.player;

/**
 * A Nope window offered to a player who did not play last. Every such player gets one, with
 * or without a Nope in hand, so how long the game waits never shows who holds a Nope.
 *
 * <p>Thread-safe: {@link #playNope()} may be called from any thread, e.g. an input or
 * network thread, at any time; late calls are refused.
 */
public interface NopeOffer extends NopeWindow {

    /**
     * Whether this player may Nope, decided when the window opened (they hold a Nope). A
     * player who can't still sees the window and its countdown.
     */
    boolean canNope();

    /**
     * Plays one of your Nope cards.
     *
     * @return {@code true} if accepted; {@code false} if you can't Nope, the window has
     *         closed, or another player was first
     */
    boolean playNope();
}
