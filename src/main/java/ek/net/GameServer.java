package ek.net;

import ek.model.CardType;

import java.io.IOException;
import java.net.*;
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

    /**
     * This machine's IPv4 addresses on active, non-loopback interfaces: candidates for remote clients
     * to connect to. Which one a client can actually reach depends on its network, so callers present
     * them as hints, not as the answer.
     */
    public static List<String> localAddresses() throws SocketException {
        return NetworkInterface.networkInterfaces()
                .filter(GameServer::isUsable)
                .flatMap(NetworkInterface::inetAddresses)
                .filter(address -> address instanceof Inet4Address && !address.isLoopbackAddress())
                .map(InetAddress::getHostAddress)
                .toList();
    }

    private static boolean isUsable(NetworkInterface nic) {
        try {
            return nic.isUp() && !nic.isLoopback();
        } catch (SocketException e) {
            return false; // the interface vanished while we looked: it can't be connected to anyway
        }
    }
}
