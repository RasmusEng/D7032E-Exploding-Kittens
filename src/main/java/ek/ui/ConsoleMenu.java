package ek.ui;

import ek.expansion.Expansion;
import ek.expansion.GameOption;
import ek.expansion.GameVariant;
import ek.rules.GameConfig;
import ek.ui.Style.Tone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * The host's setup menu: first the expansions and variants, then the number of humans and
 * bots. It lists whatever options it is given, by the names they give themselves, so a new
 * expansion needs no change here.
 *
 * <p>Only the input's form is checked here (numbers in the list, whole numbers); whether
 * the counts make a valid game is decided by {@link GameConfig}, the same check the
 * command-line shortcut uses. Bad input is explained and asked again.
 */
public final class ConsoleMenu {

    private final BufferedReader in;
    private final ConsoleOutput out;

    public ConsoleMenu(BufferedReader in, ConsoleOutput out) {
        this.in = in;
        this.out = out;
    }

    /**
     * Asks the host to set up a game.
     *
     * @param expansions the optional expansions, in the order to list them (the base game
     *                   is always included and not offered)
     * @param variants   the variants, in the order to list them
     * @param warnings   problems met while looking for options, shown with the expansions
     * @throws IllegalStateException if the input ends before the setup is complete
     */
    public GameConfig ask(List<Expansion> expansions, List<GameVariant> variants, List<String> warnings) {
        out.heading("Game setup");
        List<Expansion> chosenExpansions = choose("Expansions (the base game is always included):",
                "No expansions installed.", expansions, warnings);
        List<GameVariant> chosenVariants = choose("Variants:", "No variants installed.", variants, List.of());
        out.println("A game has " + GameConfig.playerRange(chosenExpansions)
                + " players in total. You are a human player; the other humans join over the network.");
        while (true) {
            int humans = askCount("Human players, you included: ");
            int bots = askCount("Bots: ");
            try {
                return GameConfig.of(humans, bots, chosenExpansions, chosenVariants);
            } catch (IllegalArgumentException e) {
                out.println(e.getMessage() + ". Try again.", Tone.DANGER);
            }
        }
    }

    /** Lists {@code options} and returns the ones chosen; Enter chooses none. */
    private <T extends GameOption> List<T> choose(String title, String none, List<T> options, List<String> warnings) {
        out.println(title, Tone.HEADER);
        if (options.isEmpty()) {
            out.println("  " + none, Tone.MUTED);
        }
        for (int i = 0; i < options.size(); i++) {
            GameOption option = options.get(i);
            String description = option.description().isBlank() ? "" : ": " + option.description();
            out.println("  " + (i + 1) + ") " + option.name() + description);
        }
        warnings.forEach(w -> out.println("  " + w, Tone.DANGER));
        if (options.isEmpty()) {
            return List.of();
        }
        while (true) {
            Optional<List<T>> chosen = parseChoice(ask("Numbers separated by spaces, Enter = none: "), options);
            if (chosen.isPresent()) {
                return chosen.get();
            }
            out.println("Enter numbers from 1 to " + options.size() + " separated by spaces, each once,"
                    + " or just Enter for none.");
        }
    }

    /** The options numbered in {@code line}, or empty if it isn't a list of distinct valid numbers. */
    private static <T> Optional<List<T>> parseChoice(String line, List<T> options) {
        if (line.isEmpty()) {
            return Optional.of(List.of());
        }
        Set<Integer> numbers = new LinkedHashSet<>();
        for (String token : line.split("\\s+")) {
            Optional<Integer> n = ConsoleController.parseInt(token);
            if (n.isEmpty() || n.get() < 1 || n.get() > options.size() || !numbers.add(n.get())) {
                return Optional.empty();
            }
        }
        List<T> chosen = new ArrayList<>();
        numbers.forEach(n -> chosen.add(options.get(n - 1)));
        return Optional.of(chosen);
    }

    /** A whole number, 0 or more; the game's limits are checked by {@link GameConfig}. */
    private int askCount(String prompt) {
        while (true) {
            Optional<Integer> n = ConsoleController.parseInt(ask(prompt));
            if (n.isPresent() && n.get() >= 0) {
                return n.get();
            }
            out.println("Enter a whole number, 0 or more.");
        }
    }

    /** Prompts and reads a trimmed line. */
    private String ask(String text) {
        long prompt = out.prompt(text);
        String line;
        try {
            line = in.readLine();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (line == null) {
            throw new IllegalStateException("Console input closed");
        }
        out.answered(prompt, line.trim());
        return line.trim();
    }
}
