package ek.net;

import ek.engine.GameEvent;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import ek.model.Card;
import ek.model.CardType;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.player.HandChange;
import ek.player.TurnAction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** The test plays the server's part on the other end of a {@link LoopbackChannel}. */
class GameClientTest {

    private static final GameView VIEW = new GameView(List.of(new Card(NOPE)),
            List.of(new OpponentView("Bob", 5, true)), 12, 1);

    private final RecordingListener view = new RecordingListener();
    private final List<String> namesSeen = new ArrayList<>();
    private final List<List<CardType>> nameableSeen = new ArrayList<>();
    private ScriptedController controller;
    private LoopbackChannel server;
    private CompletableFuture<String> running;

    @BeforeEach
    void setUp() {
        controller = ScriptedController.passing(3).targets(0).gives(0).names(DEFUSE);
        LoopbackChannel.Pair pair = LoopbackChannel.pair();
        server = pair.server();
        GameClient client = new GameClient(pair.client(), (name, nameable) -> {
            namesSeen.add(name);
            nameableSeen.add(nameable);
            return new GameClient.Seat(controller, view);
        }, task -> Thread.ofVirtual().start(task));
        running = CompletableFuture.supplyAsync(() -> {
            try {
                return client.run();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @Test
    void requests_arePassedToTheLocalController_andAnswered() throws Exception {
        server.send(new Message.Welcome("Ann", List.of(DEFUSE, TACO_CAT)));
        controller.play(ATTACK);

        server.send(new Message.ChooseAction(VIEW));
        assertEquals(new Message.ActionChosen(new TurnAction.Play(ATTACK)), server.receive());
        server.send(new Message.ChooseKittenPosition(VIEW, 12));
        assertEquals(new Message.NumberChosen(3), server.receive());
        server.send(new Message.ChooseTarget(VIEW, VIEW.opponents()));
        assertEquals(new Message.NumberChosen(0), server.receive());
        server.send(new Message.ChooseCardToGive(VIEW, "Bob"));
        assertEquals(new Message.NumberChosen(0), server.receive());
        server.send(new Message.NameCard(VIEW));
        assertEquals(new Message.CardNamed(DEFUSE), server.receive());

        assertEquals("Ann", endGame());
        assertEquals(List.of("Ann"), namesSeen);
        assertEquals(List.of(List.of(DEFUSE, TACO_CAT)), nameableSeen);
    }

    @Test
    void eventsAndSeeTheFuture_areShownLocally() throws Exception {
        server.send(new Message.Welcome("Ann", List.of()));
        server.send(new Message.Event(new GameEvent.Winner("Ann")));
        server.send(new Message.SeeTheFuture(List.of(new Card(SKIP))));
        server.send(new Message.HandChanged(new HandChange.Drawn(new Card(NOPE))));
        server.send(new Message.GameStarted(VIEW));

        endGame();
        assertEquals(List.of(new GameEvent.Winner("Ann")), view.events());
        assertEquals(List.of(List.of(new Card(SKIP))), controller.futuresSeen());
        assertEquals(List.of(new HandChange.Drawn(new Card(NOPE))), controller.handChanges());
        assertEquals(List.of(VIEW), controller.startViews());
    }

    @Test
    void nope_isSentToTheServer_whichDecides() throws Exception {
        server.send(new Message.Welcome("Ann", List.of()));
        controller.nopes(true);

        server.send(new Message.OfferNope(1, VIEW, "Bob", List.of(ATTACK), 0, 3000, true));
        assertEquals(new Message.PlayNope(1), server.receive());
        server.send(new Message.NopeResult(1, true));
        server.send(new Message.NopeClosed(1));

        endGame();
        var offer = controller.nopeOffers().get(0);
        assertEquals("Bob", offer.playedBy());
        assertEquals(List.of(ATTACK), offer.cards());
        assertEquals(Duration.ofSeconds(3), offer.window());
        assertTrue(offer.canNope());
        assertFalse(offer.isOpen());
    }

    @Test
    void offerWithoutANope_isShownButCantBeClaimed_andEndsWhenTheServerClosesIt() throws Exception {
        server.send(new Message.Welcome("Ann", List.of()));
        controller.nopes(true);

        server.send(new Message.OfferNope(1, VIEW, "Bob", List.of(ATTACK), 0, 3000, false));
        // nothing goes back to the server, so wait until the offer has reached the player
        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> {
            while (controller.nopeOffers().isEmpty()) {
                Thread.sleep(1);
            }
        });
        server.send(new Message.NopeClosed(1));
        endGame();

        var offer = controller.nopeOffers().get(0);
        assertFalse(offer.canNope());
        assertFalse(offer.playNope());
        assertFalse(offer.isOpen());
    }

    @Test
    void missingWelcome_isAProtocolError() throws Exception {
        server.send(new Message.ChooseAction(VIEW));

        var e = assertThrows(Exception.class, () -> running.get(5, TimeUnit.SECONDS));
        assertInstanceOf(UncheckedIOException.class, e.getCause());
    }

    /** Closes the connection like the server does after the game, and waits for the client. */
    private String endGame() throws Exception {
        server.close();
        return running.get(5, TimeUnit.SECONDS);
    }
}
