package ek.engine;

import ek.model.Player;

/**
 * What happens when a card (or a combo) is played. One class per effect, so a new card
 * means a new class and the engine stays unchanged (Open/Closed).
 *
 * <p>Lives in {@code engine} and is implemented in {@code cards} and {@code combos}, so
 * those packages depend on the engine and not the other way round.
 */
@FunctionalInterface
public interface CardEffect {

    /**
     * Applies the effect. The played cards are already on the discard pile.
     * To end the turn, the effect changes {@link GameState#turns()}.
     */
    void apply(GameContext ctx, Player actor);
}
