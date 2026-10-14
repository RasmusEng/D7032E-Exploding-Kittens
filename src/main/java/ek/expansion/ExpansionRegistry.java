package ek.expansion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Finds the optional expansions and the variants on the classpath, through
 * {@link ServiceLoader} and the files {@code META-INF/services/ek.expansion.Expansion} and
 * {@code META-INF/services/ek.expansion.GameVariant}. A new expansion jar is picked up
 * without changing any class.
 *
 * <p>This is the only class that uses {@code ServiceLoader}, and only the composition root
 * uses this class; everything else gets the chosen expansions through its constructor.
 * The base game is not looked up: it is always part of the game.
 *
 * <p>The result never depends on the order {@code ServiceLoader} finds the providers in:
 * both lists are sorted by {@link GameOption#BY_ORDER}. A provider that can't be loaded is
 * skipped and described in {@link #problems()}; it never stops the others from loading.
 */
public final class ExpansionRegistry {

    /** Stops looking if a broken classpath keeps failing, rather than looping forever. */
    private static final int MAX_FAILURES_IN_A_ROW = 50;

    private final List<String> problems = new ArrayList<>();
    private final List<Expansion> expansions;
    private final List<GameVariant> variants;

    /** Looks up the providers once, with the thread's context class loader. */
    public ExpansionRegistry() {
        expansions = load(Expansion.class, "expansion");
        variants = load(GameVariant.class, "variant");
    }

    /** The expansions the host can choose, base game excluded, in {@link GameOption#BY_ORDER}. */
    public List<Expansion> optionalExpansions() {
        return expansions;
    }

    /** The variants the host can choose, in {@link GameOption#BY_ORDER}. */
    public List<GameVariant> variants() {
        return variants;
    }

    /** One line per provider that was skipped, saying which and why; empty if all loaded. */
    public List<String> problems() {
        return List.copyOf(problems);
    }

    private <T extends GameOption> List<T> load(Class<T> service, String kind) {
        List<T> found = new ArrayList<>();
        Iterator<T> providers = ServiceLoader.load(service).iterator();
        int failuresInARow = 0;
        while (failuresInARow < MAX_FAILURES_IN_A_ROW) {
            try {
                if (!providers.hasNext()) {
                    return sorted(found);
                }
                T provider = providers.next();
                failuresInARow = 0;
                if (provider instanceof BaseGame) {
                    problems.add("Skipped a registered base game: it is always included.");
                } else {
                    found.add(provider);
                }
            } catch (ServiceConfigurationError e) {
                failuresInARow++;
                problems.add("Skipped a broken " + kind + ": " + describe(e));
            }
        }
        problems.add("Stopped looking for more " + kind + "s after " + MAX_FAILURES_IN_A_ROW + " errors in a row.");
        return sorted(found);
    }

    private static <T extends GameOption> List<T> sorted(List<T> options) {
        return options.stream().sorted(GameOption.BY_ORDER).toList();
    }

    /** The loader's message names the provider; the cause, if any, says why it failed. */
    private static String describe(ServiceConfigurationError e) {
        Throwable cause = e.getCause();
        return cause == null ? e.getMessage() : e.getMessage() + " (" + cause + ")";
    }
}
