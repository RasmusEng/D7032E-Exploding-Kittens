package ek.net;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.NopeResolver;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import ek.model.BaseCardType;
import ek.model.Player;
import ek.util.Waiter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** A whole game with one local and one remote player, connected in memory. */
class NetworkGameTest {

    private static final Executor THREADS = task -> Thread.ofVirtual().start(task);

    @Test
    void remotePlayer_nopesOverTheNetwork_andEveryoneIsToldTheWinner() throws Exception {
        LoopbackChannel.Pair pair = LoopbackChannel.pair();
        ScriptedController remotePlayer = ScriptedController.passing().nopes(true);
        RecordingListener remoteView = new RecordingListener();
        CompletableFuture<String> client = CompletableFuture.supplyAsync(() -> {
            try {
                return new GameClient(pair.client(),
                        (name, nameable) -> new GameClient.Seat(remotePlayer, remoteView), THREADS).run();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        RemoteController remote = GameServer.join(pair.server(), "B", List.of(BaseCardType.values()), THREADS);

        RecordingListener hostView = new RecordingListener();
        // window 1 is long and really waited: the test only passes quickly if the remote Nope
        // closes it. Window 2 (on B's Nope) would run its full length, so it runs out at once.
        AtomicInteger windows = new AtomicInteger();
        Waiter firstWindowReal = (signal, timeout) ->
                windows.getAndIncrement() == 0 ? Waiter.REAL.await(signal, timeout) : signal.getCount() == 0;
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().play(ATTACK), ATTACK)
                .player("B", remote, NOPE)
                .deck(SKIP, EXPLODING_KITTEN)
                .startPlayer(0)
                .nopeResolver(new NopeResolver(Duration.ofSeconds(30), THREADS, firstWindowReal))
                .listener(hostView)
                .listener(remote);

        Player winner = game.buildEngine().run();
        remote.close();

        assertEquals("B", client.get(5, TimeUnit.SECONDS));
        assertEquals("A", winner.name());
        List<GameEvent> expected = List.of(
                new GameEvent.TurnStarted("A", 1),
                new GameEvent.CardsPlayed("A", List.of(ATTACK)),
                new GameEvent.NopePlayed("B", true),
                new GameEvent.PlayCancelled("A", List.of(ATTACK)),
                new GameEvent.CardDrawn("A"),
                new GameEvent.TurnStarted("B", 1),
                new GameEvent.CardDrawn("B"),
                new GameEvent.PlayerExploded("B"),
                new GameEvent.Winner("A"));                         // R13 over the network
        assertEquals(expected, hostView.events());
        assertEquals(expected, remoteView.events());
        assertEquals(1, remotePlayer.nopeOffers().size());
    }
}
