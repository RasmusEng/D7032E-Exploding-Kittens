package ek.rules;

import ek.expansion.BaseGame;
import ek.expansion.Expansion;
import ek.expansion.GameOption;
import ek.expansion.GameVariant;
import ek.expansion.PlayerRange;

import java.util.ArrayList;
import java.util.List;

/**
 * The host's choices for one game. Both the menu and the command-line shortcut produce one,
 * and both rely on the validation here.
 *
 * @param humans     human players, the host included; at least 1
 * @param bots       bots; at least 0
 * @param expansions the base game first, then the chosen expansions in {@link GameOption#BY_ORDER}
 * @param variants   the chosen variants
 * @throws IllegalArgumentException if a value is out of range, or the total is outside the
 *                                  {@link PlayerRange} of the expansions (rule 1)
 */
public record GameConfig(int humans, int bots, List<Expansion> expansions, List<GameVariant> variants) {

    public GameConfig {
        expansions = List.copyOf(expansions);
        variants = List.copyOf(variants);
        if (expansions.isEmpty() || !(expansions.getFirst() instanceof BaseGame)) {
            throw new IllegalArgumentException("The base game must come first");
        }
        if (humans < 1) {
            throw new IllegalArgumentException("Need at least 1 human player (the host)");
        }
        if (bots < 0) {
            throw new IllegalArgumentException("The number of bots can't be negative");
        }
        PlayerRange range = PlayerRange.of(expansions);
        if (!range.contains(humans + bots)) {
            throw new IllegalArgumentException(
                    "Need " + range + " players in total, got " + (humans + bots));
        }
    }

    /** The base game plus the {@code selected} expansions, put in their fixed order. */
    public static GameConfig of(int humans, int bots, List<Expansion> selected, List<GameVariant> variants) {
        return new GameConfig(humans, bots, withBaseGame(selected),
                variants.stream().sorted(GameOption.BY_ORDER).toList());
    }

    /** How many players a game with the {@code selected} expansions (plus the base game) can have. */
    public static PlayerRange playerRange(List<Expansion> selected) {
        return PlayerRange.of(withBaseGame(selected));
    }

    /** The base game alone, without variants. */
    public static GameConfig baseGame(int humans, int bots) {
        return of(humans, bots, List.of(), List.of());
    }

    private static List<Expansion> withBaseGame(List<Expansion> selected) {
        List<Expansion> expansions = new ArrayList<>();
        expansions.add(new BaseGame());
        selected.stream().sorted(GameOption.BY_ORDER).forEach(expansions::add);
        return expansions;
    }

    /** Humans and bots together. */
    public int players() {
        return humans + bots;
    }
}
