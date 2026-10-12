package ek.cards;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.Player;

/** Rule 11e: show the player the top 3 cards (fewer if the deck is short), privately. */
public final class SeeTheFutureEffect implements CardEffect {

    private static final int CARDS_SHOWN = 3;

    @Override
    public void apply(GameContext ctx, Player actor) {
        ctx.controllerOf(actor).seeTheFuture(ctx.state().deck().peek(CARDS_SHOWN));
    }
}
