package ek.expansion;

import java.util.Comparator;

/**
 * Something the host can choose before a game: an {@link Expansion} or a {@link GameVariant}.
 * The menu shows only what an option says about itself, so a new option needs no change to
 * the menu. Every method has a default, which keeps {@code Expansion} a functional interface.
 */
public interface GameOption {

    /**
     * The order options are listed and applied in, by {@link #order()}, then name, then class
     * name. It is total, so the result never depends on the order the options were found in.
     */
    Comparator<GameOption> BY_ORDER = Comparator.comparingInt(GameOption::order)
            .thenComparing(GameOption::name)
            .thenComparing(o -> o.getClass().getName());

    /** Name shown in the menu; defaults to the class's simple name. */
    default String name() {
        return getClass().getSimpleName();
    }

    /** One line about the option, shown in the menu; empty by default. */
    default String description() {
        return "";
    }

    /**
     * Position among the options, lowest first. Expansions are applied in this order (after
     * the base game), so one that adjusts another's cards needs a higher number than it.
     */
    default int order() {
        return 0;
    }
}
