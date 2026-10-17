package ek.cards;

import ek.engine.DrawEffect;
import ek.engine.GameContext;
import ek.engine.GameEvent;
import ek.model.Card;
import ek.model.Player;

import static ek.model.BaseCardType.DEFUSE;

/**
 * Rule 10: a drawn Exploding Kitten is defused (the Defuse is discarded and the kitten goes
 * back where the player chooses), or the player explodes.
 */
public final class ExplodingKittenDraw implements DrawEffect {

    @Override
    public Outcome apply(GameContext ctx, Player actor, Card kitten, boolean faceUp) {
        if (!actor.hand().contains(DEFUSE)) {
            ctx.explode(actor, kitten);                                         // R10b
            return Outcome.EXPLODED;
        }
        ctx.state().discardPile().add(actor.hand().remove(DEFUSE));             // R10a
        ctx.state().deck().insertAt(ctx.chooseDeckPosition(actor), kitten);
        ctx.publish(new GameEvent.KittenDefused(actor));
        return Outcome.SURVIVED;
    }
}
