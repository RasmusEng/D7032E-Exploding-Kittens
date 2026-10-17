package ek.cards;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.engine.TurnState;
import ek.model.Player;

import java.util.List;

/** Rule 11a: end the turn without drawing; the next player takes 2 turns (stacking). */
public final class AttackEffect implements CardEffect {

    @Override
    public void apply(GameContext ctx, Player actor) {
        TurnState turns = ctx.state().turns();
        List<Player> players = ctx.state().players();
        turns.attack(players, turns.nextAlive(players));
    }
}
