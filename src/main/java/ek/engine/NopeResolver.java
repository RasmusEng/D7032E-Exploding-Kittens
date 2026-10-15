package ek.engine;

import ek.model.CardType;
import ek.model.Player;
import ek.player.GameView;
import ek.player.NopeOffer;
import ek.player.NopeWindow;
import ek.player.PlayerController;
import ek.util.Waiter;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

import static ek.model.BaseCardType.NOPE;

/**
 * Rule 12: after a play, opens a Nope window that always runs its full length (5 seconds)
 * and is shown to every player. It opens even if nobody holds a Nope and can't be declined
 * or skipped, so how long the game waits never shows who holds one. Only a Nope ends it
 * early, and that opens a new full window on the Nope (Yup) for everyone but the player who
 * Noped. The play is canceled when the number of Nopes is odd.
 *
 * <p><b>Thread-safety.</b> {@link #isCancelled} runs on the engine thread and is the only
 * code here that changes game state; who may Nope is decided there too, when the window
 * opens. Offers are sent to controllers through the injected {@link Executor}, so players
 * decide in parallel. They answer by calling {@link NopeOffer#playNope()} from any thread;
 * that only updates the synchronized {@link Window}, and the engine thread then moves the
 * Nope card.
 */
public final class NopeResolver {

    /** Rule 12: a Nope must be played within 5 seconds. */
    public static final Duration DEFAULT_WINDOW = Duration.ofSeconds(5);

    private final Duration window;
    private final Executor executor;
    private final Waiter waiter;

    /**
     * @param window   how long every window lasts unless someone Nopes
     * @param executor runs the {@code offerNope} and {@code watchNopeWindow} calls; tests pass {@code Runnable::run}
     */
    public NopeResolver(Duration window, Executor executor) {
        this(window, executor, Waiter.REAL);
    }

    /** @param waiter waits out each window; tests pass one that lets the time run out at once */
    public NopeResolver(Duration window, Executor executor, Waiter waiter) {
        this.window = window;
        this.executor = executor;
        this.waiter = waiter;
    }

    /**
     * Runs Nope windows until one runs out without a Nope. Played Nopes go to the discard pile.
     *
     * @return {@code true} if the play is canceled (odd number of Nopes)
     */
    public boolean isCancelled(GameContext ctx, Player actor, List<CardType> cards) {
        int nopes = 0;
        Player lastPlayed = actor;
        Player noper;
        while ((noper = openWindow(ctx, lastPlayed, actor, cards, nopes)) != null) {
            ctx.state().discardPile().add(noper.hand().remove(NOPE));
            nopes++;
            ctx.publish(new GameEvent.NopePlayed(noper, nopes % 2 == 1));
            lastPlayed = noper;
        }
        return nopes % 2 == 1;
    }

    /**
     * Shows the window to {@code lastPlayed} and offers it to every other alive player, then
     * waits its full length or until someone Nopes. Returns the player who Noped, or
     * {@code null} if the time ran out.
     */
    private Player openWindow(GameContext ctx, Player lastPlayed, Player actor,
                              List<CardType> cards, int nopes) {
        Window w = new Window(actor.name(), cards, nopes, window);
        for (Player p : ctx.state().alivePlayers()) {
            GameView view = ctx.viewFor(p);          // built on the engine thread
            PlayerController controller = ctx.controllerOf(p);
            if (p == lastPlayed) {
                NopeWindow watched = w.offerFor(p, false);
                executor.execute(() -> controller.watchNopeWindow(view, watched));
            } else {
                NopeOffer offer = w.offerFor(p, p.hand().contains(NOPE));
                executor.execute(() -> controller.offerNope(view, offer));
            }
        }
        return w.awaitClaim(waiter);
    }

    /**
     * One Nope window. The first claim from a player who may Nope wins; claims after closing
     * are refused. Only a claim wakes the engine before the time is up.
     */
    private static final class Window {

        private final String playedBy;
        private final List<CardType> cards;
        private final int nopesSoFar;
        private final Duration length;
        private final CountDownLatch claimed = new CountDownLatch(1);
        private Player claimer;           // guarded by this
        private boolean open = true;      // guarded by this

        Window(String playedBy, List<CardType> cards, int nopesSoFar, Duration length) {
            this.playedBy = playedBy;
            this.cards = List.copyOf(cards);
            this.nopesSoFar = nopesSoFar;
            this.length = length;
        }

        synchronized boolean claim(Player player) {
            if (!open || claimer != null) {
                return false;
            }
            claimer = player;
            claimed.countDown();
            return true;
        }

        /** Waits until claimed or the full length has passed, then closes the window. */
        Player awaitClaim(Waiter waiter) {
            try {
                waiter.await(claimed, length);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();   // keep the flag; close the window as timed out
            }
            return close();
        }

        private synchronized Player close() {
            open = false;
            return claimer;
        }

        synchronized boolean isOpen() {
            return open && claimer == null;
        }

        /** @param canNope whether {@code player} holds a Nope; decided on the engine thread */
        NopeOffer offerFor(Player player, boolean canNope) {
            return new NopeOffer() {
                public String playedBy() {
                    return playedBy;
                }

                public List<CardType> cards() {
                    return cards;
                }

                public int nopesSoFar() {
                    return nopesSoFar;
                }

                public Duration window() {
                    return length;
                }

                public boolean isOpen() {
                    return Window.this.isOpen();
                }

                public boolean canNope() {
                    return canNope;
                }

                public boolean playNope() {
                    return canNope && claim(player);
                }
            };
        }
    }
}
