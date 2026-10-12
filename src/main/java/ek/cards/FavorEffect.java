package ek.cards;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.Player;

/** Rule 11b: another player gives you 1 card of their choice (nothing if their hand is empty). */
public final class FavorEffect implements CardEffect {

    @Override
    public void apply(GameContext ctx, Player actor) {
        Player target = ctx.chooseTarget(actor);
        int handSize = target.hand().size();
        if (handSize == 0) {
            return;
        }
        int index = GameContext.askInRange(
                () -> ctx.controllerOf(target).chooseCardToGive(ctx.viewFor(target), actor.name()),
                0, handSize - 1);
        ctx.giveCard(target.hand().removeAt(index), target, actor);
    }
}
