package ek.net;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Fake connection: two linked in-memory channels, no sockets. Closing either end makes
 * both ends read end-of-stream. {@link #receive()} gives up after a few seconds, so a
 * broken test fails instead of hanging.
 */
public final class LoopbackChannel implements MessageChannel {

    private static final Object END = new Object();
    private static final long TIMEOUT_SECONDS = 5;

    private final BlockingQueue<Object> inbox;
    private final BlockingQueue<Object> outbox;
    private volatile boolean closed;

    private LoopbackChannel(BlockingQueue<Object> inbox, BlockingQueue<Object> outbox) {
        this.inbox = inbox;
        this.outbox = outbox;
    }

    /** Two connected ends: what one sends, the other receives. */
    public static Pair pair() {
        BlockingQueue<Object> toServer = new LinkedBlockingQueue<>();
        BlockingQueue<Object> toClient = new LinkedBlockingQueue<>();
        return new Pair(new LoopbackChannel(toServer, toClient), new LoopbackChannel(toClient, toServer));
    }

    public record Pair(LoopbackChannel server, LoopbackChannel client) {
    }

    @Override
    public void send(Message message) throws IOException {
        if (closed) {
            throw new IOException("closed");
        }
        outbox.add(message);
    }

    @Override
    public Message receive() throws IOException {
        try {
            Object next = inbox.poll(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (next == null) {
                throw new IOException("Test timed out waiting for a message");
            }
            if (next == END) {
                inbox.add(END);     // stay at end-of-stream
                throw new EOFException();
            }
            return (Message) next;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        closed = true;
        inbox.add(END);
        outbox.add(END);
    }
}
