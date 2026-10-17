package ek.fakeimploding;

import ek.engine.CardEffect;
import ek.engine.GameContext;
import ek.model.Deck;
import ek.model.Player;

/** Like Draw from the Bottom: end the turn by drawing the bottom card, which still does what drawing it does. */
public final class FakeDrawFromTheBottom implements CardEffect {

    @Override
    public void apply(GameContext ctx, Player actor) {
        ctx.drawAndEndTurn(actor, Deck.Side.BOTTOM);
    }
}