package ek.cards;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.Player;

/** Rule 11a: end the turn without drawing; the next player takes 2 turns (stacking). */
public final class AttackEffect implements CardEffect {

    @Override
    public void apply(GameContext ctx, Player actor) {
        ctx.state().turns().attack(ctx.state().players());
    }
}
