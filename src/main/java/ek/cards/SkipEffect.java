package ek.cards;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.Player;

/** Rule 11d: end one turn without drawing. */
public final class SkipEffect implements CardEffect {

    @Override
    public void apply(GameContext ctx, Player actor) {
        ctx.state().turns().endTurn(ctx.state().players());
    }
}
