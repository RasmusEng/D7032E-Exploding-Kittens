package ek.net;

import ek.engine.GameEventListener;
import ek.model.CardType;
import ek.net.Message.ToClient;
import ek.player.NopeOffer;
import ek.player.PlayerController;

import java.io.EOFException;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The client end of a network game. Passes each message from the server to a local
 * {@link PlayerController} and {@link GameEventListener} (normally the console ones,
 * built by a {@link SeatFactory} once the server has named the player) and
 * sends the answers back. It knows no card names or keywords: everything it shows comes
 * from the server (rule 17).
 *
 * <p><b>Thread-safety.</b> {@link #run()} reads messages on the calling thread. Nope offers
 * run on the injected executor so the reader can still receive {@code NopeClosed} and
 * {@code NopeResult} for them; offer state is volatile or in concurrent maps.
 */
public final class GameClient {

    /** The local player's controller and the view that shows them the game's events. */
    public record Seat(PlayerController controller, GameEventListener view) {
    }

    /** Builds the {@link Seat} once the server's welcome says who this player is. */
    @FunctionalInterface
    public interface SeatFactory {

        /**
         * @param name          the name the server gave this player
         * @param nameableCards the cards that may be named for three of a kind
         */
        Seat create(String name, List<CardType> nameableCards);
    }

    private final MessageChannel channel;
    private final SeatFactory seatFactory;
    private final Executor nopeExecutor;
    private final Map<Integer, ClientNopeOffer> offers = new ConcurrentHashMap<>();
    private PlayerController controller;
    private GameEventListener view;

    /** @param nopeExecutor runs Nope offers; each needs its own thread */
    public GameClient(MessageChannel channel, SeatFactory seatFactory, Executor nopeExecutor) {
        this.channel = channel;
        this.seatFactory = seatFactory;
        this.nopeExecutor = nopeExecutor;
    }

    /**
     * Plays until the server ends the game by closing the connection.
     *
     * @return the name the server gave this player
     * @throws IOException if the connection fails or the server breaks the protocol
     */
    public String run() throws IOException {
        if (!(channel.receive() instanceof Message.Welcome welcome)) {
            throw new StreamCorruptedException("Expected a welcome message");
        }
        Seat seat = seatFactory.create(welcome.name(), welcome.nameableCards());
        controller = seat.controller();
        view = seat.view();
        try {
            while (true) {
                handle(receive());
            }
        } catch (EOFException e) {
            return welcome.name();     // the server closed the connection: the game is over
        } finally {
            offers.values().forEach(ClientNopeOffer::end);
        }
    }

    private void handle(ToClient message) throws IOException {
        switch (message) {
            case Message.Welcome w -> throw new StreamCorruptedException("Welcome sent twice");
            case Message.Event e -> view.onEvent(e.event());
            case Message.ChooseAction r -> channel.send(new Message.ActionChosen(controller.chooseAction(r.view())));
            case Message.ChooseKittenPosition r -> channel.send(
                    new Message.NumberChosen(controller.chooseKittenPosition(r.view(), r.deckSize())));
            case Message.ChooseTarget r -> channel.send(
                    new Message.NumberChosen(controller.chooseTarget(r.view(), r.candidates())));
            case Message.ChooseCardToGive r -> channel.send(
                    new Message.NumberChosen(controller.chooseCardToGive(r.view(), r.requester())));
            case Message.NameCard r -> channel.send(new Message.CardNamed(controller.nameCard(r.view())));
            case Message.SeeTheFuture s -> controller.seeTheFuture(s.topCards());
            case Message.HandChanged h -> controller.handChanged(h.change());
            case Message.OfferNope o -> startOffer(o);
            case Message.NopeClosed c -> {
                ClientNopeOffer offer = offers.get(c.offerId());
                if (offer != null) {
                    offer.close();
                }
            }
            case Message.NopeResult r -> {
                ClientNopeOffer offer = offers.get(r.offerId());
                if (offer != null) {
                    offer.result.complete(r.accepted());
                }
            }
        }
    }

    private ToClient receive() throws IOException {
        if (channel.receive() instanceof ToClient message) {
            return message;
        }
        throw new StreamCorruptedException("Server sent a client message");
    }

    private void startOffer(Message.OfferNope message) {
        ClientNopeOffer offer = new ClientNopeOffer(message);
        offers.put(message.offerId(), offer);
        nopeExecutor.execute(() -> {
            try {
                controller.offerNope(message.view(), offer);
            } finally {
                offers.remove(message.offerId());
            }
        });
    }

    /** The client's side of a server Nope window. Playing Nope asks the server, which decides. */
    private final class ClientNopeOffer implements NopeOffer {

        private final Message.OfferNope message;
        private final CompletableFuture<Boolean> result = new CompletableFuture<>();
        private final AtomicBoolean played = new AtomicBoolean();
        private volatile boolean open = true;

        ClientNopeOffer(Message.OfferNope message) {
            this.message = message;
        }

        @Override
        public String playedBy() {
            return message.playedBy();
        }

        @Override
        public List<CardType> cards() {
            return message.cards();
        }

        @Override
        public int nopesSoFar() {
            return message.nopesSoFar();
        }

        @Override
        public Duration window() {
            return Duration.ofMillis(message.windowMillis());
        }

        @Override
        public boolean isOpen() {
            return open;
        }

        /** Asks the server and waits for its answer. Only the first call is sent. */
        @Override
        public boolean playNope() {
            if (!open || !played.compareAndSet(false, true)) {
                return false;
            }
            try {
                channel.send(new Message.PlayNope(message.offerId()));
            } catch (IOException e) {
                return false;
            }
            return result.join();
        }

        void close() {
            open = false;
        }

        /** The connection is gone: close the offer and release a waiting {@link #playNope()}. */
        void end() {
            close();
            result.complete(false);
        }
    }
}
