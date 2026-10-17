package ek.fakeimploding;

import ek.engine.DrawEffect;
import ek.engine.GameContext;
import ek.model.Card;
import ek.model.Player;

/**
 * Like the Imploding Kitten: drawn face down, it goes back into the deck face up where the
 * player chooses; drawn face up, the player is out, and a Defuse can't stop it.
 */
public final class FakeImplodingKittenDraw implements DrawEffect {

    @Override
    public Outcome apply(GameContext ctx, Player actor, Card kitten, boolean faceUp) {
        if (faceUp) {
            ctx.eliminate(actor, kitten);
            return Outcome.EXPLODED;
        }
        ctx.putBackFaceUp(actor, kitten);
        return Outcome.SURVIVED;
    }
}