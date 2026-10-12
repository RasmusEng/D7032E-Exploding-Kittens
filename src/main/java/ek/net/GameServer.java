package ek.net;

import ek.model.CardType;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.Executor;

/** Lets remote players join a game: one {@link RemoteController} per connected client. */
public final class GameServer {

    /** The TCP port the server listens on and clients connect to. */
    public static final int PORT = 2048;

    private GameServer() {
    }

    /** Waits for the next client to connect and seats it as {@code name}. */
    public static RemoteController accept(ServerSocket server, String name, List<CardType> nameableCards,
                                          Executor readers) throws IOException {
        return join(new ObjectStreamChannel(server.accept()), name, nameableCards, readers);
    }

    /**
     * Welcomes a connected client and starts reading its messages.
     *
     * @param readers runs the controller's reader loop; it needs its own thread
     */
    public static RemoteController join(MessageChannel channel, String name, List<CardType> nameableCards,
                                        Executor readers) throws IOException {
        channel.send(new Message.Welcome(name, nameableCards));
        RemoteController controller = new RemoteController(name, channel);
        readers.execute(controller::listen);
        return controller;
    }
}
