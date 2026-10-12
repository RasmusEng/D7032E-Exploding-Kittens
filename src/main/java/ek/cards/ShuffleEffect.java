package ek.cards;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.Player;

/** Rule 11c: shuffle the deck. */
public final class ShuffleEffect implements CardEffect {

    @Override
    public void apply(GameContext ctx, Player actor) {
        ctx.state().deck().shuffle(ctx.randomizer());
    }
}
