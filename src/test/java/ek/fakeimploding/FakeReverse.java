package ek.fakeimploding;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.Player;

/** Like Reverse: play changes direction and one turn ends without drawing. */
public final class FakeReverse implements CardEffect {

    @Override
    public void apply(GameContext ctx, Player actor) {
        ctx.state().turns().reverse();
        ctx.state().turns().endTurn(ctx.state().players());
    }
}