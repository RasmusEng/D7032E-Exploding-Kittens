package ek.fakeimploding;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.Player;

/** Like Targeted Attack: a chosen player takes 2 turns (stacking as Attack); play continues from them. */
public final class FakeTargetedAttack implements CardEffect {

    @Override
    public void apply(GameContext ctx, Player actor) {
        Player victim = ctx.chooseTarget(actor);
        ctx.state().turns().attack(ctx.state().players(), victim);
    }
}