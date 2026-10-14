package ek.net;

import ek.engine.GameEvent;
import ek.model.Card;
import ek.model.CardType;
import ek.player.GameView;
import ek.player.HandChange;
import ek.player.NopeOffer;
import ek.player.TurnAction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** The test plays the client's part on the other end of a {@link LoopbackChannel}. */
class RemoteControllerTest {

    private static final GameView VIEW = new GameView(List.of(), List.of(), 10, 1);

    private LoopbackChannel client;
    private RemoteController remote;
    private Thread reader;

    @BeforeEach
    void connect() {
        LoopbackChannel.Pair pair = LoopbackChannel.pair();
        client = pair.client();
        remote = new RemoteController("Ann", pair.server());
        reader = Thread.ofVirtual().start(remote::listen);
    }

    @AfterEach
    void disconnect() throws Exception {
        remote.close();
        reader.join();
    }

    @Test
    void chooseAction_sendsRequest_andReturnsClientsAnswer() throws IOException {
        client.send(new Message.ActionChosen(new TurnAction.Play(TACO_CAT, TACO_CAT)));

        assertEquals(new TurnAction.Play(TACO_CAT, TACO_CAT), remote.chooseAction(VIEW));
        assertEquals(new Message.ChooseAction(VIEW), client.receive());
    }

    @Test
    void numberAndCardAnswers_reachTheEngine() throws IOException {
        client.send(new Message.NumberChosen(4));
        client.send(new Message.CardNamed(DEFUSE));

        assertEquals(4, remote.chooseKittenPosition(VIEW, 10));
        assertEquals(DEFUSE, remote.nameCard(VIEW));
        assertEquals(new Message.ChooseKittenPosition(VIEW, 10), client.receive());
        assertEquals(new Message.NameCard(VIEW), client.receive());
    }

    @Test
    void eventsAndSeeTheFuture_areForwarded() throws IOException {
        remote.onEvent(new GameEvent.Winner("Ann"));
        remote.seeTheFuture(List.of());
        remote.handChanged(new HandChange.Lost(new Card(SKIP), "Bob"));
        remote.gameStarted(VIEW);

        assertEquals(new Message.Event(new GameEvent.Winner("Ann")), client.receive());
        assertEquals(new Message.SeeTheFuture(List.of()), client.receive());
        assertEquals(new Message.HandChanged(new HandChange.Lost(new Card(SKIP), "Bob")), client.receive());
        assertEquals(new Message.GameStarted(VIEW), client.receive());
    }

    @Test
    void answerOfWrongType_isAProtocolError() throws IOException {
        client.send(new Message.NumberChosen(1));

        assertThrows(UncheckedIOException.class, () -> remote.chooseAction(VIEW));
    }

    @Test
    void disconnectedClient_failsEveryLaterRequest() {
        client.close();

        UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> remote.chooseAction(VIEW));
        assertTrue(e.getMessage().contains("Ann disconnected"));
        assertThrows(UncheckedIOException.class, () -> remote.nameCard(VIEW));
    }

    @Test
    void nopeInTime_claimsTheWindow_andClientIsToldBeforeClose() throws Exception {
        FakeOffer offer = new FakeOffer();
        Thread window = Thread.ofVirtual().start(() -> remote.offerNope(VIEW, offer));

        Message.OfferNope sent = (Message.OfferNope) client.receive();
        assertEquals(new Message.OfferNope(sent.offerId(), VIEW, "Bob", List.of(ATTACK), 0, 5000, true), sent);
        client.send(new Message.PlayNope(sent.offerId()));

        assertEquals(new Message.NopeResult(sent.offerId(), true), client.receive());
        assertEquals(new Message.NopeClosed(sent.offerId()), client.receive());
        window.join();
        assertEquals(1, offer.claims);
    }

    @Test
    void offerWithoutANope_isSent_andANopeOnItIsRefused() throws Exception {
        FakeOffer offer = new FakeOffer(false);
        Thread window = Thread.ofVirtual().start(() -> remote.offerNope(VIEW, offer));
        Message.OfferNope sent = (Message.OfferNope) client.receive();
        assertFalse(sent.canNope());

        client.send(new Message.PlayNope(sent.offerId()));
        assertEquals(new Message.NopeResult(sent.offerId(), false), client.receive());

        offer.closeWindow();
        assertEquals(new Message.NopeClosed(sent.offerId()), client.receive());
        window.join();
        assertEquals(0, offer.claims);
    }

    @Test
    void lateNope_isRefused() throws Exception {
        FakeOffer offer = new FakeOffer();
        Thread window = Thread.ofVirtual().start(() -> remote.offerNope(VIEW, offer));
        int id = ((Message.OfferNope) client.receive()).offerId();

        offer.closeWindow();
        assertEquals(new Message.NopeClosed(id), client.receive());
        client.send(new Message.PlayNope(id));

        assertEquals(new Message.NopeResult(id, false), client.receive());
        window.join();
        assertEquals(0, offer.claims);
    }

    @Test
    void closedOffer_isReportedBeforeTheNextMessage() throws Exception {
        FakeOffer offer = new FakeOffer();
        Thread window = Thread.ofVirtual().start(() -> remote.offerNope(VIEW, offer));
        int id = ((Message.OfferNope) client.receive()).offerId();

        offer.closeWindow();
        remote.onEvent(new GameEvent.CardDrawn("Bob"));     // sent before the poll may notice

        assertEquals(new Message.NopeClosed(id), client.receive());
        assertEquals(new Message.Event(new GameEvent.CardDrawn("Bob")), client.receive());
        window.join();
    }

    @Test
    void offerForAnAlreadyClosedWindow_isNotSent() throws IOException {
        FakeOffer offer = new FakeOffer();
        offer.closeWindow();

        remote.offerNope(VIEW, offer);
        remote.onEvent(new GameEvent.CardDrawn("Bob"));

        assertEquals(new Message.Event(new GameEvent.CardDrawn("Bob")), client.receive());
    }

    /** A Nope window controlled by the test. */
    private static final class FakeOffer implements NopeOffer {

        private final boolean canNope;
        private boolean open = true;
        private int claims;

        FakeOffer() {
            this(true);
        }

        FakeOffer(boolean canNope) {
            this.canNope = canNope;
        }

        @Override
        public String playedBy() {
            return "Bob";
        }

        @Override
        public List<CardType> cards() {
            return List.of(ATTACK);
        }

        @Override
        public int nopesSoFar() {
            return 0;
        }

        @Override
        public Duration window() {
            return Duration.ofSeconds(5);
        }

        @Override
        public synchronized boolean isOpen() {
            return open;
        }

        @Override
        public boolean canNope() {
            return canNope;
        }

        @Override
        public synchronized boolean playNope() {
            if (!canNope || !open) {
                return false;
            }
            open = false;
            claims++;
            return true;
        }

        synchronized void closeWindow() {
            open = false;
        }
    }
}
