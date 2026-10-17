package ek.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.Socket;

/**
 * Sends {@link Message}s with Java serialization over a pair of streams, normally a socket.
 *
 * <p>Only game types and basic JDK types may be read ({@link #FILTER}), so a hostile
 * peer cannot make this side create arbitrary classes.
 *
 * <p><b>Thread-safety.</b> {@link #send} is synchronized; {@link #receive} must be called
 * from one thread at a time.
 */
public final class ObjectStreamChannel implements MessageChannel {

    /**
     * Classes allowed on the wire: game types, records' JDK parts, and immutable lists.
     * "Game types" means everything under {@code ek.}, so an expansion's card types must live
     * in an {@code ek.} package (e.g. {@code ek.imploding}) to reach the clients.
     */
    static final ObjectInputFilter FILTER = ObjectInputFilter.Config.createFilter(
            "maxdepth=20;maxarray=1000;maxrefs=10000;ek.**;java.lang.*;java.util.*;!*");

    private final ObjectOutputStream out;
    private final ObjectInputStream in;
    private final Closeable resource;

    public ObjectStreamChannel(Socket socket) throws IOException {
        this(socket.getInputStream(), socket.getOutputStream(), socket);
    }

    /**
     * Writes the stream header before reading the other side's, so two channels created
     * at the same time on both ends don't wait for each other.
     */
    public ObjectStreamChannel(InputStream in, OutputStream out, Closeable resource) throws IOException {
        this.out = new ObjectOutputStream(out);
        this.out.flush();
        this.in = new ObjectInputStream(in);
        this.in.setObjectInputFilter(FILTER);
        this.resource = resource;
    }

    @Override
    public synchronized void send(Message message) throws IOException {
        out.writeObject(message);
        out.flush();
        out.reset();     // don't keep references to sent objects
    }

    /** @throws java.io.InvalidObjectException if a record constructor rejected the received values */
    @Override
    public Message receive() throws IOException {
        try {
            if (in.readObject() instanceof Message message) {
                return message;
            }
            throw new StreamCorruptedException("Received an object that is not a Message");
        } catch (ClassNotFoundException e) {
            throw new InvalidClassException("Unknown class on the wire: " + e.getMessage());
        }
    }

    @Override
    public void close() throws IOException {
        resource.close();
    }
}
