package ek.combos;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.CardType;
import ek.model.Player;

/** Rule 11g: name a card and take it from another player, if they have it. */
public final class ThreeOfAKind implements CardEffect {
    @Override
    public void apply(GameContext ctx, Player actor) {
        Player target = ctx.chooseTarget(actor);
        CardType named = ctx.controllerOf(actor).nameCard(ctx.viewFor(actor));
        if (named == null || !target.hand().contains(named)) {
            return;
        }
        ctx.giveCard(target.hand().remove(named), target, actor);
    }
}