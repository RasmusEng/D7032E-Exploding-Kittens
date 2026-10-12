package ek.net;

import java.io.Closeable;
import java.io.IOException;

/**
 * A two-way connection that carries {@link Message}s. The rest of the network code only
 * sees this interface, so the transport and wire format can change without touching it
 * (rule 17); tests use an in-memory implementation.
 *
 * <p>Implementations must allow {@link #send} from several threads at once, and one
 * thread calling {@link #receive} at the same time.
 */
public interface MessageChannel extends Closeable {

    void send(Message message) throws IOException;

    /**
     * Waits for the next message.
     *
     * @throws java.io.EOFException if the other side closed the connection
     */
    Message receive() throws IOException;
}
