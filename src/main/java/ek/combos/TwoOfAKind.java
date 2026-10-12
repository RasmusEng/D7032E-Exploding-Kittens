package ek.combos;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.Player;

/** Rule 11f: steal a uniformly random card from another player. */
public final class TwoOfAKind implements CardEffect {

    @Override
    public void apply(GameContext ctx, Player actor) {
        Player target = ctx.chooseTarget(actor);
        int handSize = target.hand().size();
        if (handSize == 0) {
            return;
        }
        ctx.giveCard(target.hand().removeAt(ctx.randomizer().nextInt(handSize)), target, actor);
    }
}
