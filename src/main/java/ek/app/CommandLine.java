package ek.app;

import ek.rules.GameConfig;

/**
 * What the command line asks for. Parsing is kept apart from {@link Main} so it can be
 * tested without exiting the JVM.
 * <ul>
 *   <li>{@code server}: open the setup menu, where expansions and variants are chosen;</li>
 *   <li>{@code server <humans> <bots>}: a shortcut for the base game only. It never offers
 *       the installed expansions or variants, so the shortcut always means the same game;</li>
 *   <li>{@code client <ip>}: join a game.</li>
 * </ul>
 */
sealed interface CommandLine {

    String USAGE = """
            Usage:
              java -cp out/classes ek.app.Main server                     set up a game in a menu
              java -cp out/classes ek.app.Main server <humans> <bots>     base game, no menu
              java -cp out/classes ek.app.Main client <ip>                join a game
            humans = human players (you + humans-1 clients), bots = computer players.""";

    /** Open the setup menu. */
    record OpenMenu() implements CommandLine {
    }

    /** Host the base game with these players, without the menu. */
    record HostBaseGame(GameConfig config) implements CommandLine {
    }

    /** Join the game hosted at {@code host}. */
    record Join(String host) implements CommandLine {
    }

    /** The arguments make no sense; {@code message} says why. */
    record Invalid(String message) implements CommandLine {
    }

    static CommandLine parse(String... args) {
        if (args.length == 1 && args[0].equals("server")) {
            return new OpenMenu();
        }
        if (args.length == 3 && args[0].equals("server")) {
            return baseGame(args[1], args[2]);
        }
        if (args.length == 2 && args[0].equals("client")) {
            return new Join(args[1]);
        }
        return new Invalid(USAGE);
    }

    private static CommandLine baseGame(String humans, String bots) {
        try {
            return new HostBaseGame(GameConfig.baseGame(Integer.parseInt(humans), Integer.parseInt(bots)));
        } catch (NumberFormatException e) {
            return new Invalid(USAGE);
        } catch (IllegalArgumentException e) {
            return new Invalid(e.getMessage() + ".");
        }
    }
}
