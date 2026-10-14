package ek.engine;

import ek.model.CardType;
import ek.player.NopeOffer;
import ek.util.Waiter;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class NopeResolverTest {

    private static List<CardType> types(GameStateBuilder game, int player) {
        return game.player(player).hand().cards().stream().map(c -> c.type()).toList();
    }

    // ---- Rule 12 through the engine ----

    @Test
    void rule12_nope_cancelsAction() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().play(ATTACK), ATTACK)
                .player("B", ScriptedController.passing().nopes(true), NOPE)
                .deck(SKIP, EXPLODING_KITTEN)
                .listener(listener);
        // Attack is Noped: A's turn goes on, A passes and draws SKIP; B draws the kitten
        assertEquals(game.player(0), game.buildEngine().run());

        assertEquals(List.of(SKIP), types(game, 0));
        assertEquals(List.of(new GameEvent.NopePlayed(game.player(1), true)),
                listener.eventsOf(GameEvent.NopePlayed.class));
        assertEquals(1, listener.eventsOf(GameEvent.PlayCancelled.class).size());
        assertEquals(List.of(ATTACK, NOPE), game.state().discardPile().cards().stream()
                .map(c -> c.type()).toList().subList(0, 2));      // Noped card stays discarded
    }

    @Test
    void rule12a_nopeOnNope_isYup_actionHappens() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().play(SKIP).nopes(true), SKIP, NOPE)
                .player("B", ScriptedController.passing().nopes(true), NOPE)
                .deck(EXPLODING_KITTEN)
                .listener(listener);
        // B Nopes A's Skip, A Nopes back (Yup): Skip happens, so B draws the kitten
        assertEquals(game.player(0), game.buildEngine().run());

        assertEquals(List.of(new GameEvent.NopePlayed(game.player(1), true),
                        new GameEvent.NopePlayed(game.player(0), false)),
                listener.eventsOf(GameEvent.NopePlayed.class));
        assertTrue(listener.eventsOf(GameEvent.PlayCancelled.class).isEmpty());
    }

    @Test
    void rule12a_threeNopes_actionCancelled() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().play(SKIP).nopes(true), SKIP, NOPE)
                .player("B", ScriptedController.passing().nopes(true), NOPE)
                // C is offered every window: B is first in window 1, A in window 2, C Nopes in window 3
                .player("C", ScriptedController.passing().nopes(false, false, true), NOPE)
                .deck(SKIP, EXPLODING_KITTEN, EXPLODING_KITTEN);
        RecordingListener listener = new RecordingListener();
        game.listener(listener).buildEngine().run();

        // B Nopes, A Yups, C Nopes again: Skip cancelled, so A draws instead of skipping
        assertEquals(3, listener.eventsOf(GameEvent.NopePlayed.class).size());
        assertEquals(1, listener.eventsOf(GameEvent.PlayCancelled.class).size());
        assertEquals(game.player(0).name(), listener.eventsOf(GameEvent.CardDrawn.class).get(0).player());
    }

    @Test
    void rule12_comboCanBeNoped() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().play(TACO_CAT, TACO_CAT), TACO_CAT, TACO_CAT)
                .player("B", ScriptedController.passing().nopes(true), NOPE)
                .deck(SKIP, EXPLODING_KITTEN);
        // no target is ever asked (script has none): the steal never happens
        game.buildEngine().run();
        assertEquals(List.of(SKIP), types(game, 0));
    }

    @Test
    void rule12_offeredToEveryOtherAlivePlayer_withOrWithoutNope_lastPlayerWatches() {
        ScriptedController a = ScriptedController.passing().play(SHUFFLE);
        ScriptedController b = ScriptedController.passing();
        ScriptedController c = ScriptedController.passing();
        new GameStateBuilder()
                .player("A", a, SHUFFLE, NOPE)
                .player("B", b, NOPE)
                .player("C", c, SKIP)
                .deck(SKIP, EXPLODING_KITTEN, EXPLODING_KITTEN)
                .buildEngine().run();

        assertTrue(a.nopeOffers().isEmpty());     // can't Nope your own play...
        assertEquals(1, a.watchedWindows().size());   // ...but sees the window
        assertEquals(1, b.nopeOffers().size());
        assertEquals(1, c.nopeOffers().size());   // offered without a Nope, so timing reveals nothing
        assertTrue(b.nopeOffers().get(0).canNope());
        assertFalse(c.nopeOffers().get(0).canNope());
        NopeOffer offer = b.nopeOffers().get(0);
        assertEquals("A", offer.playedBy());
        assertEquals(List.of(SHUFFLE), offer.cards());
        assertEquals(0, offer.nopesSoFar());
        assertEquals("A", a.watchedWindows().get(0).playedBy());
    }

    @Test
    void offer_tellsTheWindowLength_forACountdown() {
        ScriptedController b = ScriptedController.passing();
        new GameStateBuilder()
                .player("A", ScriptedController.passing().play(SHUFFLE), SHUFFLE)
                .player("B", b, NOPE)
                .deck(SKIP, EXPLODING_KITTEN, EXPLODING_KITTEN)
                .nopeResolver(new NopeResolver(Duration.ofMillis(7), Runnable::run))
                .buildEngine().run();

        assertEquals(Duration.ofMillis(7), b.nopeOffers().get(0).window());
    }

    @Test
    void rule12_drawingAndDefusing_cannotBeNoped() {
        ScriptedController b = ScriptedController.passing();
        new GameStateBuilder()
                .player("A", ScriptedController.passing(0), DEFUSE)
                .player("B", b, NOPE)
                .deck(EXPLODING_KITTEN)
                .buildEngine().run();
        assertTrue(b.nopeOffers().isEmpty());
    }

    @Test
    void rule12_nopeCannotBePlayedOnItsOwn() {
        RecordingListener listener = new RecordingListener();
        new GameStateBuilder()
                .player("A", ScriptedController.passing().play(NOPE), NOPE)
                .player("B", ScriptedController.passing())
                .deck(SKIP, EXPLODING_KITTEN)
                .listener(listener)
                .buildEngine().run();
        assertEquals(1, listener.eventsOf(GameEvent.MoveRejected.class).size());
    }

    @Test
    void rule12_windowIsFiveSeconds() {
        assertEquals(Duration.ofSeconds(5), NopeResolver.DEFAULT_WINDOW);
    }

    // ---- The window itself ----

    @Test
    void nopeAfterWindowClosed_refused() {
        ScriptedController b = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", b, NOPE);
        GameContext ctx = game.buildContext();

        assertFalse(GameStateBuilder.instantNopeResolver().isCancelled(ctx, game.player(0), List.of(SKIP)));
        assertFalse(b.nopeOffers().get(0).playNope());
        assertTrue(game.player(1).hand().contains(NOPE));
    }

    @Test
    void onlyFirstNopeInAWindowCounts() {
        ScriptedController b = ScriptedController.passing().nopes(true);
        ScriptedController c = ScriptedController.passing().nopes(true);
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", b, NOPE)
                .player("C", c, NOPE);
        GameContext ctx = game.buildContext();

        assertTrue(GameStateBuilder.instantNopeResolver().isCancelled(ctx, game.player(0), List.of(SKIP)));
        // B was first; C's Nope was refused and C keeps the card. Next window: C may Yup B (script says no more)
        assertFalse(game.player(1).hand().contains(NOPE));
        assertTrue(game.player(2).hand().contains(NOPE));
    }

    @Test
    void windowOpensEvenWhenNobodyHoldsNope_andRunsItsFullLength() {
        ScriptedController b = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", b);
        RecordingWaiter waiter = new RecordingWaiter();
        NopeResolver resolver = new NopeResolver(NopeResolver.DEFAULT_WINDOW, Runnable::run, waiter);

        assertFalse(resolver.isCancelled(game.buildContext(), game.player(0), List.of(SKIP)));
        assertEquals(List.of(NopeResolver.DEFAULT_WINDOW), waiter.waits);
        assertEquals(1, b.nopeOffers().size());
        assertFalse(b.nopeOffers().get(0).canNope());
    }

    @Test
    void windowNotClosedEarly_whenPlayersDontAnswer() {
        ScriptedController b = ScriptedController.passing();   // holds a Nope, never answers
        ScriptedController c = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", b, NOPE)
                .player("C", c);
        RecordingWaiter waiter = new RecordingWaiter();
        List<Boolean> openWhileWaiting = new ArrayList<>();
        waiter.duringWait = () -> {
            openWhileWaiting.add(b.nopeOffers().getLast().isOpen());
            openWhileWaiting.add(c.nopeOffers().getLast().isOpen());
        };
        NopeResolver resolver = new NopeResolver(NopeResolver.DEFAULT_WINDOW, Runnable::run, waiter);

        assertFalse(resolver.isCancelled(game.buildContext(), game.player(0), List.of(SKIP)));
        // every offer had been handled, yet the engine still waited the whole window
        assertEquals(List.of(NopeResolver.DEFAULT_WINDOW), waiter.waits);
        assertEquals(List.of(true, true), openWhileWaiting);
        assertFalse(b.nopeOffers().getLast().isOpen());
        assertTrue(game.player(1).hand().contains(NOPE));
    }

    @Test
    void rule12a_nope_opensANewFullWindow_forEveryoneButTheNoper() {
        ScriptedController a = ScriptedController.passing();
        ScriptedController b = ScriptedController.passing().nopes(true);
        ScriptedController c = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", a, NOPE)
                .player("B", b, NOPE)
                .player("C", c);
        RecordingWaiter waiter = new RecordingWaiter();
        NopeResolver resolver = new NopeResolver(NopeResolver.DEFAULT_WINDOW, Runnable::run, waiter);

        assertTrue(resolver.isCancelled(game.buildContext(), game.player(0), List.of(SKIP)));
        // window 1 ended by B's Nope; window 2 ran its full length again
        assertEquals(2, waiter.waits.size());
        assertEquals(List.of(NopeResolver.DEFAULT_WINDOW, NopeResolver.DEFAULT_WINDOW), waiter.waits);
        // window 2: A (who played) may now Yup, B (who Noped) watches, C is offered again
        assertEquals(1, a.nopeOffers().size());
        assertEquals(1, a.nopeOffers().get(0).nopesSoFar());
        assertTrue(a.nopeOffers().get(0).canNope());
        assertEquals(1, b.nopeOffers().size());
        assertEquals(1, b.watchedWindows().size());
        assertEquals(2, c.nopeOffers().size());
        assertEquals(1, a.watchedWindows().size());               // window 1 was on A's own play
        assertEquals(0, a.watchedWindows().get(0).nopesSoFar());
        assertEquals(1, b.watchedWindows().get(0).nopesSoFar());  // window 2 was on B's Nope
    }

    @Test
    void playerWithoutNope_cannotClaimTheWindow() {
        ScriptedController b = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", b, SKIP);
        RecordingWaiter waiter = new RecordingWaiter();
        List<Boolean> claims = new ArrayList<>();
        waiter.duringWait = () -> claims.add(b.nopeOffers().getLast().playNope());
        NopeResolver resolver = new NopeResolver(NopeResolver.DEFAULT_WINDOW, Runnable::run, waiter);

        assertFalse(resolver.isCancelled(game.buildContext(), game.player(0), List.of(SKIP)));
        assertEquals(List.of(false), claims);
        assertEquals(1, waiter.waits.size());      // no Nope, so no second window
        assertEquals(List.of(SKIP), types(game, 1));
    }

    @Test
    void nopeFromAnotherThread_wakesEngineBeforeTimeout() {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            ScriptedController b = ScriptedController.passing().nopes(true);   // runs on an executor thread
            GameStateBuilder game = new GameStateBuilder()
                    .player("A", ScriptedController.passing())
                    .player("B", b, NOPE);
            // window 1 really waits (an hour) until B's Nope wakes it; window 2 runs out at once
            AtomicInteger windows = new AtomicInteger();
            Waiter firstWindowReal = (signal, timeout) ->
                    windows.getAndIncrement() == 0 ? Waiter.REAL.await(signal, timeout) : signal.getCount() == 0;
            NopeResolver resolver = new NopeResolver(Duration.ofHours(1), executor, firstWindowReal);
            GameContext ctx = game.buildContext();

            assertTimeoutPreemptively(Duration.ofSeconds(5),
                    () -> assertTrue(resolver.isCancelled(ctx, game.player(0), List.of(SKIP))));
            assertFalse(game.player(1).hand().contains(NOPE));
        } finally {
            executor.shutdownNow();
        }
    }

    /** Lets every window run out at once, recording how long the engine asked to wait. */
    private static final class RecordingWaiter implements Waiter {

        final List<Duration> waits = new ArrayList<>();
        Runnable duringWait = () -> {
        };

        @Override
        public boolean await(CountDownLatch signal, Duration timeout) {
            waits.add(timeout);
            duringWait.run();
            return signal.getCount() == 0;
        }
    }
}
