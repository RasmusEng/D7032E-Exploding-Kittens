package ek.app;

import ek.engine.GameEventListener;
import ek.expansion.ExpansionRegistry;
import ek.model.CardType;
import ek.model.Player;
import ek.net.GameClient;
import ek.net.GameServer;
import ek.net.ObjectStreamChannel;
import ek.net.RemoteController;
import ek.player.BotController;
import ek.player.PlayerController;
import ek.rules.GameConfig;
import ek.ui.ConsoleController;
import ek.ui.ConsoleMenu;
import ek.ui.ConsoleOutput;
import ek.ui.ConsoleView;
import ek.ui.Style;
import ek.util.JavaRandomizer;
import ek.util.Randomizer;
import ek.util.Sleeper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Composition root: the only place that creates the services and wires them together.
 *
 * The command line is described in {@link CommandLine}.
 */
public final class Main {

    /** Runs each task on a new virtual thread: Nope offers and network readers block. */
    private static final Executor VIRTUAL_THREADS = task -> Thread.ofVirtual().start(task);

    public static void main(String[] args) {
        switch (CommandLine.parse(args)) {
            case CommandLine.OpenMenu menu -> hostWithMenu();
            case CommandLine.HostBaseGame host -> runServer(host.config(), consoleIn(), consoleOut());
            case CommandLine.Join join -> runClient(join.host());
            case CommandLine.Invalid invalid -> exit(invalid.message());
        }
    }

    /**
     * Lets the host choose the expansions, variants and players, then hosts the game. Only
     * here is the registry used; everything below gets the chosen lists through the config.
     */
    private static void hostWithMenu() {
        ExpansionRegistry registry = new ExpansionRegistry();
        BufferedReader in = consoleIn();
        ConsoleOutput console = consoleOut();
        GameConfig config = new ConsoleMenu(in, console)
                .ask(registry.optionalExpansions(), registry.variants(), registry.problems());
        runServer(config, in, console);
    }

    /** Hosts a game: this console is Player 1, the other humans connect as clients. */
    private static void runServer(GameConfig config, BufferedReader in, ConsoleOutput console) {
        Randomizer randomizer = new JavaRandomizer();
        GameAssembler assembler = new GameAssembler(config, randomizer);
        List<CardType> nameable = assembler.nameableCards();

        Map<Player, PlayerController> controllers = new LinkedHashMap<>();
        List<GameEventListener> listeners = new ArrayList<>();
        Player host = new Player("Player 1");
        controllers.put(host, new ConsoleController(in, console, nameable, Sleeper.REAL));
        List<RemoteController> remotes;
        try {
            remotes = acceptClients(config.humans() - 1, nameable);
        } catch (IOException e) {
            exit("Could not start the server: " + e.getMessage());
            return;
        }
        for (RemoteController remote : remotes) {
            controllers.put(new Player(remote.name()), remote);
            listeners.add(remote);
        }

        listeners.add(new ConsoleView(console, host.name(), Sleeper.REAL));
        for (int i = 1; i <= config.bots(); i++) {
            controllers.put(new Player("Bot " + i),
                    new BotController(randomizer, assembler.playableCards(), nameable, Sleeper.REAL));
        }

        try (ExecutorService nopeExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            assembler.assemble(controllers, listeners, nopeExecutor).run();
        } catch (UncheckedIOException e) {
            System.err.println("Game aborted: " + e.getMessage());
        } finally {
            remotes.forEach(Main::closeQuietly);
        }
    }

    /** Waits for {@code count} clients; they are named Player 2, Player 3, ... in joining order. */
    private static List<RemoteController> acceptClients(int count, List<CardType> nameable) throws IOException {
        List<RemoteController> remotes = new ArrayList<>();
        if (count == 0) {
            return remotes;
        }
        try (ServerSocket server = new ServerSocket(GameServer.PORT)) {
            System.out.println("Waiting for " + count + " player(s) on port " + GameServer.PORT + "...");
            System.out.println("To join, run: java -cp out/classes ek.app.Main client <ip>");
            printJoinAddresses();
            for (int i = 2; i <= count + 1; i++) {
                remotes.add(GameServer.accept(server, "Player " + i, nameable, VIRTUAL_THREADS));
                System.out.println("Player " + i + " joined.");
            }
        }
        return remotes;
    }

    /** Joins a game hosted at {@code host} and plays it at this console. */
    private static void runClient(String host) {
        try (Socket socket = new Socket(host, GameServer.PORT)) {
            System.out.println("Connected to " + host + ". Waiting for the game to start...");
            BufferedReader in = consoleIn();
            ConsoleOutput console = consoleOut();
            String name = new GameClient(new ObjectStreamChannel(socket),
                    (me, nameable) -> new GameClient.Seat(new ConsoleController(in, console, nameable, Sleeper.REAL),
                            new ConsoleView(console, me, Sleeper.NONE)),
                    VIRTUAL_THREADS).run();
            System.out.println("Game over, " + name + ".");
        } catch (IOException e) {
            exit("Connection to " + host + " failed: " + e.getMessage());
        }
    }

    /** Lists this machine's addresses as hints; the right one depends on where the client is. */
    private static void printJoinAddresses() {
        System.out.println("  on this computer: 127.0.0.1");
        try {
            List<String> addresses = GameServer.localAddresses();
            if (!addresses.isEmpty()) {
                System.out.println("  from another computer, try: " + String.join(", ", addresses));
            }
        } catch (SocketException e) {
            System.out.println("  (could not list this computer's addresses: " + e.getMessage() + ")");
        }
    }

    /** Create once per run and share: a second reader on System.in could miss buffered input. */
    private static BufferedReader consoleIn() {
        return new BufferedReader(new InputStreamReader(System.in));
    }

    private static ConsoleOutput consoleOut() {
        return new ConsoleOutput(System.out, Style.detect(System.getenv()));
    }

    private static void closeQuietly(RemoteController remote) {
        try {
            remote.close();
        } catch (IOException e) {
            System.err.println("Could not close a connection: " + e.getMessage());
        }
    }

    private static void exit(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
