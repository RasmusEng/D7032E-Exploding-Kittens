package ek.engine;

import ek.model.CardType;
import ek.model.Player;
import ek.player.GameView;
import ek.player.NopeOffer;
import ek.player.PlayerController;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import static ek.model.BaseCardType.NOPE;

/**
 * Rule 12: after a play, gives every player holding a Nope a timed window to stop it.
 * The window closes early once everyone offered it has declined, so the game only waits
 * as long as somebody is still deciding. A Nope on a Nope reverses it (Yup), and each
 * Nope opens a new window, so the play is cancelled when the number of Nopes is odd.
 *
 * <p><b>Thread-safety.</b> {@link #isCancelled} runs on the engine thread and is the only
 * code here that changes game state. Offers are sent to controllers through the injected
 * {@link Executor}, so players decide in parallel. They answer by calling
 * {@link NopeOffer#playNope()} or {@link NopeOffer#decline()} from any thread; that only
 * updates the synchronized {@link Window}, and the engine thread then moves the Nope card.
 */
public final class NopeResolver {

    /** Rule 12: a Nope must be played within 5 seconds. */
    public static final Duration DEFAULT_WINDOW = Duration.ofSeconds(5);

    private final Duration window;
    private final Executor executor;

    /**
     * @param window   how long players have to Nope
     * @param executor runs the {@code offerNope} calls; tests pass {@code Runnable::run}
     */
    public NopeResolver(Duration window, Executor executor) {
        this.window = window;
        this.executor = executor;
    }

    /**
     * Runs Nope windows until nobody Nopes. Played Nopes go to the discard pile.
     *
     * @return {@code true} if the play is cancelled (odd number of Nopes)
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
     * Offers a Nope to every alive player holding one, except whoever played last,
     * and waits for the first taker. Returns {@code null} if nobody Nopes in time or all decline.
     */
    private Player openWindow(GameContext ctx, Player lastPlayed, Player actor,
                              List<CardType> cards, int nopes) {
        List<Player> eligible = ctx.state().alivePlayers().stream()
                .filter(p -> p != lastPlayed && p.hand().contains(NOPE))
                .toList();
        if (eligible.isEmpty()) {
            return null;
        }
        Window w = new Window(actor.name(), cards, nopes, window, eligible.size());
        for (Player p : eligible) {
            GameView view = ctx.viewFor(p);          // built on the engine thread
            PlayerController controller = ctx.controllerOf(p);
            NopeOffer offer = w.offerFor(p);
            executor.execute(() -> controller.offerNope(view, offer));
        }
        return w.awaitClaim(window);
    }

    /**
     * One Nope window. The first claim wins; claims after closing are refused. It is
     * decided, and the engine woken, by a claim or by the last of the offered players declining.
     */
    private static final class Window {

        private final String playedBy;
        private final List<CardType> cards;
        private final int nopesSoFar;
        private final Duration length;
        private final int offered;
        private final CountDownLatch decided = new CountDownLatch(1);
        private final Set<Player> declined = new HashSet<>();   // guarded by this
        private Player claimer;           // guarded by this
        private boolean open = true;      // guarded by this

        Window(String playedBy, List<CardType> cards, int nopesSoFar, Duration length, int offered) {
            this.playedBy = playedBy;
            this.cards = List.copyOf(cards);
            this.nopesSoFar = nopesSoFar;
            this.length = length;
            this.offered = offered;
        }

        synchronized boolean claim(Player player) {
            if (!open || claimer != null || declined.contains(player)) {
                return false;
            }
            claimer = player;
            decided.countDown();
            return true;
        }

        synchronized void decline(Player player) {
            if (open && claimer == null && declined.add(player) && declined.size() == offered) {
                decided.countDown();
            }
        }

        /** Waits until claimed, everyone declined, or the time is up, then closes the window. */
        Player awaitClaim(Duration timeout) {
            try {
                decided.await(timeout.toNanos(), TimeUnit.NANOSECONDS);
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

        NopeOffer offerFor(Player player) {
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

                public boolean playNope() {
                    return claim(player);
                }

                public void decline() {
                    Window.this.decline(player);
                }
            };
        }
    }
}
