package ek.net;

import ek.engine.GameEvent;
import ek.model.BaseCardType;
import ek.model.Card;
import ek.player.CardChoice;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.player.TurnAction;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.StreamCorruptedException;
import java.time.LocalDate;
import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class ObjectStreamChannelTest {

    private static final GameView VIEW = new GameView(List.of(new Card(NOPE), new Card(TACO_CAT)),
            List.of(new OpponentView("Bob", 3, true), new OpponentView("Cy", 0, false)), 20, 2);

    @Test
    void everyMessage_survivesTheRoundTrip() throws IOException {
        List<Message> messages = List.of(
                new Message.Welcome("Ann", List.of(BaseCardType.values())),
                new Message.Event(new GameEvent.CardsPlayed("Bob", List.of(TACO_CAT, TACO_CAT))),
                new Message.Event(new GameEvent.Winner("Ann")),
                new Message.ChooseAction(VIEW),
                new Message.ChooseKittenPosition(VIEW, 20),
                new Message.ChooseTarget(VIEW, VIEW.opponents()),
                new Message.ChooseCardToGive(VIEW, "Bob"),
                new Message.NameCard(VIEW),
                new Message.ChooseCards(VIEW, CardChoice.orderOf("Order", List.of(new Card(SKIP), new Card(NOPE)))),
                new Message.CardsChosen(List.of(1, 0)),
                new Message.GameStarted(VIEW),
                new Message.SeeTheFuture(List.of(new Card(EXPLODING_KITTEN), new Card(SKIP))),
                new Message.OfferNope(7, VIEW, "Bob", List.of(ATTACK), 1, 5000, true),
                new Message.WatchNopeWindow(8, VIEW, "Ann", List.of(ATTACK), 0, 5000),
                new Message.NopeClosed(7),
                new Message.NopeResult(7, true),
                new Message.ActionChosen(new TurnAction.Pass()),
                new Message.ActionChosen(new TurnAction.Play(TACO_CAT, TACO_CAT)),
                new Message.NumberChosen(3),
                new Message.CardNamed(DEFUSE),
                new Message.PlayNope(7));
        try (ObjectStreamChannel channel = selfConnected()) {
            for (Message message : messages) {
                channel.send(message);
                assertEquals(message, channel.receive());
            }
        }
    }

    @Test
    void classNotOnTheWhitelist_isRejected() throws IOException {
        ObjectStreamChannel channel = reading(LocalDate.of(2026, 1, 1));    // java.time is not allowed

        assertThrows(InvalidClassException.class, channel::receive);
    }

    @Test
    void allowedObjectThatIsNotAMessage_isRejected() throws IOException {
        assertThrows(StreamCorruptedException.class, reading("just a string")::receive);
    }

    @Test
    void closedStream_readsAsEndOfStream() throws IOException {
        ObjectStreamChannel channel = new ObjectStreamChannel(
                new ByteArrayInputStream(header()), new ByteArrayOutputStream(), () -> { });

        assertThrows(EOFException.class, channel::receive);
    }

    /** A channel whose output is piped straight back into its input. */
    private static ObjectStreamChannel selfConnected() throws IOException {
        PipedInputStream in = new PipedInputStream(1 << 16);
        PipedOutputStream out = new PipedOutputStream(in);
        return new ObjectStreamChannel(in, out, () -> {
            in.close();
            out.close();
        });
    }

    /** A channel that will read exactly this object, written by a plain object stream. */
    private static ObjectStreamChannel reading(Object object) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(object);
        }
        return new ObjectStreamChannel(new ByteArrayInputStream(bytes.toByteArray()),
                new ByteArrayOutputStream(), () -> { });
    }

    private static byte[] header() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new ObjectOutputStream(bytes).flush();
        return bytes.toByteArray();
    }
}
