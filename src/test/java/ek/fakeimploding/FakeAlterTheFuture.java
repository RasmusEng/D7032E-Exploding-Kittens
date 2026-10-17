package ek.fakeimploding;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.Card;
import ek.model.Player;
import ek.player.CardChoice;

import java.util.List;

/** Like Alter the Future: the player puts the top 3 cards (fewer if the deck is short) in a new order. */
public final class FakeAlterTheFuture implements CardEffect {

    private static final int CARDS = 3;

    @Override
    public void apply(GameContext ctx, Player actor) {
        List<Card> top = ctx.state().deck().peek(CARDS);
        if (top.isEmpty()) {
            return;
        }
        List<Integer> order = ctx.chooseCards(actor, CardChoice.orderOf("Put the top cards in a new order, top first:", top));
        ctx.state().deck().reorderTop(order);
    }
}