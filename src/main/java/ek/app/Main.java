package ek.app;

import ek.combos.ThreeOfAKind;
import ek.combos.TwoOfAKind;
import ek.engine.CardEffect;
import ek.engine.GameEngine;
import ek.engine.GameEventListener;
import ek.engine.GameState;
import ek.engine.NopeResolver;
import ek.engine.PlayResolver;
import ek.expansion.BaseGame;
import ek.expansion.DeckFactory;
import ek.expansion.Expansion;
import ek.model.BaseCardType;
import ek.model.CardType;
import ek.model.Deck;
import ek.model.DiscardPile;
import ek.model.Player;
import ek.net.GameClient;
import ek.net.GameServer;
import ek.net.ObjectStreamChannel;
import ek.net.RemoteController;
import ek.player.BotController;
import ek.player.PlayerController;
import ek.rules.GameSetup;
import ek.ui.ConsoleController;
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
 * <pre>
 * java -cp out/classes ek.app.Main server &lt;numPlayers&gt; &lt;numBots&gt;
 * java -cp out/classes ek.app.Main client &lt;ip&gt;
 * </pre>
 */
public final class Main {

    private static final String USAGE = """
            Invalid start try:
              java -cp out/classes ek.app.Main server <numPlayers> <numBots>
              java -cp out/classes ek.app.Main client <ip>
            numPlayers = human players (you + numPlayers-1 clients), numBots = bots; 2-5 in total.
            Max total players = 5
            """;

    /** Runs each task on a new virtual thread: Nope offers and network readers block. */
    private static final Executor VIRTUAL_THREADS = task -> Thread.ofVirtual().start(task);

    public static void main(String[] args) {
        startParameters(args);
    }

    /** Parses the command line and starts a server or a client, or exist */
    private static void startParameters(String[] args) {
        if (args.length == 3 && args[0].equals("server")) {
            Integer humans = parseCount(args[1]);
            Integer bots = parseCount(args[2]);
            if (humans == null || bots == null || humans < 1) {
                exit(USAGE);
            } else if (humans + bots < GameSetup.MIN_PLAYERS || humans + bots > GameSetup.MAX_PLAYERS) {
                exit("Need " + GameSetup.MIN_PLAYERS + "-" + GameSetup.MAX_PLAYERS + " players in total.");
            } else {
                runServer(humans, bots);
            }
        } else if (args.length == 2 && args[0].equals("client")) {
            runClient(args[1]);
        } else {
            exit(USAGE);
        }
    }

    /** Hosts a game: this console is Player 1, the other humans connect as clients. */
    private static void runServer(int humans, int bots) {
        Randomizer randomizer = new JavaRandomizer();
        List<Expansion> expansions = List.of(new BaseGame());
        Map<CardType, CardEffect> effects = new LinkedHashMap<>();
        expansions.forEach(e -> effects.putAll(e.cardEffects())); // Kollar expansions och laddar in de
        List<CardType> nameable = List.of(BaseCardType.values());

        Map<Player, PlayerController> controllers = new LinkedHashMap<>();
        List<GameEventListener> listeners = new ArrayList<>();
        ConsoleOutput console = new ConsoleOutput(System.out, Style.detect(System.getenv()));
        Player host = new Player("Player 1");
        controllers.put(host, new ConsoleController(
                new BufferedReader(new InputStreamReader(System.in)), console, nameable));
        List<RemoteController> remotes;
        try {
            remotes = acceptClients(humans - 1, nameable);
        } catch (IOException e) {
            exit("Could not start the server: " + e.getMessage());
            return;
        }
        for (RemoteController remote : remotes) {
            controllers.put(new Player(remote.name()), remote);
            listeners.add(remote);
        }

        listeners.add(new ConsoleView(console, host.name(), Sleeper.REAL));
        for (int i = 1; i <= bots; i++) {
            controllers.put(new Player("Bot " + i), new BotController(randomizer, effects.keySet(), nameable));
        }
        List<Player> players = new ArrayList<>(controllers.keySet());

        Deck deck;
        try {
            deck = new GameSetup(new DeckFactory(expansions), randomizer).setUp(players);
        } catch (IllegalArgumentException e) {
            exit(e.getMessage());
            return;
        }
        try (ExecutorService nopeExecutor = Executors.newVirtualThreadPerTaskExecutor()) {
            new GameEngine(
                    new GameState(players, deck, new DiscardPile()),
                    controllers,
                    new PlayResolver(effects, new TwoOfAKind(), new ThreeOfAKind()),
                    new NopeResolver(NopeResolver.DEFAULT_WINDOW, nopeExecutor),
                    randomizer,
                    listeners).run();
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
            BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
            ConsoleOutput console = new ConsoleOutput(System.out, Style.detect(System.getenv()));
            String name = new GameClient(new ObjectStreamChannel(socket),
                    (me, nameable) -> new GameClient.Seat(new ConsoleController(in, console, nameable),
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

    private static void closeQuietly(RemoteController remote) {
        try {
            remote.close();
        } catch (IOException e) {
            System.err.println("Could not close a connection: " + e.getMessage());
        }
    }

    /** A non-negative number, or {@code null} if {@code s} isn't one. */
    private static Integer parseCount(String s) {
        try {
            int n = Integer.parseInt(s);
            return n >= 0 ? n : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static void exit(String message) {
        System.err.println(message);
        System.exit(1);
    }
}
