package ek.net;

import ek.engine.GameEvent;
import ek.engine.GameEventListener;
import ek.model.Card;
import ek.model.CardType;
import ek.net.Message.ToClient;
import ek.net.Message.ToServer;
import ek.player.CardChoice;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.player.HandChange;
import ek.player.NopeOffer;
import ek.player.NopeWindow;
import ek.player.PlayerController;
import ek.player.TurnAction;

import java.io.Closeable;
import java.io.IOException;
import java.io.StreamCorruptedException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;

/**
 * Server-side stand-in for a player on another machine (Proxy). Each controller call and
 * each game event becomes a {@link Message} to the client; the engine can't tell this
 * player from a local one. One object does both jobs so that everything this client
 * receives goes through one ordered stream.
 *
 * <p><b>Thread-safety.</b> Requests and events come from the engine thread, Nope offers
 * from Nope-window threads, and {@link #listen()} runs on its own reader thread. Answers
 * reach the engine thread through a {@link BlockingQueue}. All sends hold {@code sendLock},
 * and before any other message the closed Nope offers are reported as closed, so the
 * client never sees an old Nope prompt after a newer message. If the client disconnects,
 * the next request throws {@link UncheckedIOException}.
 */
public final class RemoteController implements PlayerController, GameEventListener, Closeable {

    private static final long NOPE_POLL_MILLIS = 50;

    private final String name;
    private final MessageChannel channel;
    private final BlockingQueue<Optional<ToServer>> answers = new LinkedBlockingQueue<>();   // empty = disconnected
    private final Map<Integer, Shown> openOffers = new ConcurrentHashMap<>();
    private final AtomicInteger nextOfferId = new AtomicInteger();
    private final Object sendLock = new Object();

    /** A window shown to the client; {@code offer} is null for a watched one, which can't be claimed. */
    private record Shown(NopeWindow window, NopeOffer offer) {
    }

    /** @param name the player's name, used in error messages */
    public RemoteController(String name, MessageChannel channel) {
        this.name = name;
        this.channel = channel;
    }

    /** The name the client was welcomed with. */
    public String name() {
        return name;
    }

    /** Reads the client's messages until it disconnects. Run this on its own thread. */
    public void listen() {
        try {
            while (true) {
                if (!(channel.receive() instanceof ToServer message)) {
                    throw new StreamCorruptedException("Client sent a server message");
                }
                switch (message) {
                    case Message.PlayNope nope -> answerNope(nope.offerId());
                    case Message.ActionChosen a -> answers.add(Optional.of(a));
                    case Message.NumberChosen n -> answers.add(Optional.of(n));
                    case Message.CardNamed c -> answers.add(Optional.of(c));
                    case Message.CardsChosen c -> answers.add(Optional.of(c));
                }
            }
        } catch (IOException | UncheckedIOException e) {
            answers.add(Optional.empty());
        }
    }

    @Override
    public TurnAction chooseAction(GameView view) {
        return ask(new Message.ChooseAction(view), Message.ActionChosen.class).action();
    }

    @Override
    public int chooseKittenPosition(GameView view, int deckSize) {
        return ask(new Message.ChooseKittenPosition(view, deckSize), Message.NumberChosen.class).number();
    }

    @Override
    public int chooseTarget(GameView view, List<OpponentView> candidates) {
        return ask(new Message.ChooseTarget(view, candidates), Message.NumberChosen.class).number();
    }

    @Override
    public int chooseCardToGive(GameView view, String requester) {
        return ask(new Message.ChooseCardToGive(view, requester), Message.NumberChosen.class).number();
    }

    @Override
    public CardType nameCard(GameView view) {
        return ask(new Message.NameCard(view), Message.CardNamed.class).type();
    }

    @Override
    public List<Integer> chooseCards(GameView view, CardChoice choice) {
        return ask(new Message.ChooseCards(view, choice), Message.CardsChosen.class).indexes();
    }

    @Override
    public void seeTheFuture(List<Card> topCards) {
        send(new Message.SeeTheFuture(topCards));
    }

    @Override
    public void gameStarted(GameView view) {
        send(new Message.GameStarted(view));
    }

    @Override
    public void handChanged(HandChange change) {
        send(new Message.HandChanged(change));
    }

    @Override
    public void onEvent(GameEvent event) {
        send(new Message.Event(event));
    }

    /**
     * Sends the offer, then waits on this (Nope-window) thread until the window closes,
     * so the client is told at once when time is up. Because it blocks, the Nope executor
     * must run offers on their own threads.
     */
    @Override
    public void offerNope(GameView view, NopeOffer offer) {
        show(new Shown(offer, offer), id -> new Message.OfferNope(id, view, offer.playedBy(), offer.cards(),
                offer.nopesSoFar(), offer.window().toMillis(), offer.canNope()));
    }

    /** Like {@link #offerNope}, for the window on the client's own play: it can't be claimed. */
    @Override
    public void watchNopeWindow(GameView view, NopeWindow window) {
        show(new Shown(window, null), id -> new Message.WatchNopeWindow(id, view, window.playedBy(), window.cards(),
                window.nopesSoFar(), window.window().toMillis()));
    }

    /** Sends the window under a new id and blocks until it closes, then sends {@code NopeClosed}. */
    private void show(Shown shown, IntFunction<ToClient> message) {
        NopeWindow window = shown.window();
        int id = nextOfferId.incrementAndGet();
        try {
            synchronized (sendLock) {
                if (!window.isOpen()) {
                    return;            // too late: the game has moved on
                }
                openOffers.put(id, shown);
                channel.send(message.apply(id));
            }
            while (window.isOpen() && openOffers.containsKey(id) && pause()) {
                // wait for the window to close
            }
            synchronized (sendLock) {
                closeOffer(id);
            }
        } catch (IOException e) {
            openOffers.remove(id);     // the engine thread notices the disconnect on its next send
        }
    }

    /** Closes the connection; the client sees the end of the game. */
    @Override
    public void close() throws IOException {
        channel.close();
    }

    /** Sends a request and waits for the answer, which must be of the expected type. */
    private <A extends ToServer> A ask(ToClient request, Class<A> answerType) {
        send(request);
        ToServer answer = takeAnswer();
        if (!answerType.isInstance(answer)) {
            throw new UncheckedIOException(new StreamCorruptedException(name + " sent "
                    + answer.getClass().getSimpleName() + " instead of " + answerType.getSimpleName()));
        }
        return answerType.cast(answer);
    }

    private ToServer takeAnswer() {
        try {
            Optional<ToServer> answer = answers.take();
            if (answer.isEmpty()) {
                answers.add(answer);   // stay disconnected for later requests
                throw new UncheckedIOException(new IOException(name + " disconnected"));
            }
            return answer.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for " + name, e);
        }
    }

    private void send(ToClient message) {
        try {
            synchronized (sendLock) {
                for (Map.Entry<Integer, Shown> offer : openOffers.entrySet()) {
                    if (!offer.getValue().window().isOpen()) {
                        closeOffer(offer.getKey());
                    }
                }
                channel.send(message);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(name + " disconnected", e);
        }
    }

    /** Tells the client an offer closed, once. Caller holds {@code sendLock}. */
    private void closeOffer(int id) throws IOException {
        if (openOffers.remove(id) != null) {
            channel.send(new Message.NopeClosed(id));
        }
    }

    /**
     * Runs on the reader thread: claims the Nope window and tells the client if it was in time.
     * A watched window (the client's own play) can't be claimed. Holding {@code sendLock}
     * while claiming makes the result reach the client before the engine's next message about it.
     */
    private void answerNope(int id) throws IOException {
        synchronized (sendLock) {
            Shown shown = openOffers.get(id);
            boolean accepted = shown != null && shown.offer() != null && shown.offer().playNope();
            channel.send(new Message.NopeResult(id, accepted));
        }
    }

    /** @return false if interrupted (flag restored) */
    private static boolean pause() {
        try {
            Thread.sleep(NOPE_POLL_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
