package ek.engine;

import ek.model.Card;
import ek.model.Player;
import ek.player.HandChange;

/**
 * What happens when a card is drawn at the end of a turn. Cards without one simply go into
 * the hand ({@link #KEEP}); kittens get their own, so a new kind of kitten (e.g. from an
 * expansion) is a new class and the engine stays unchanged (Open/Closed).
 *
 * <p>Like {@link CardEffect}, it lives in {@code engine} and is implemented elsewhere.
 */
@FunctionalInterface
public interface DrawEffect {

    /**
     * Whether the player who drew survived or is out of the game (exploded or otherwise);
     * {@link GameContext#drawAndEndTurn} moves the turn on accordingly.
     */
    enum Outcome { SURVIVED, EXPLODED }

    /** The default: the card goes into the hand, and only the drawer is told which card it was. */
    DrawEffect KEEP = (ctx, actor, card, faceUp) -> {
        actor.hand().add(card);
        ctx.controllerOf(actor).handChanged(new HandChange.Drawn(card));
        return Outcome.SURVIVED;
    };

    /**
     * Handles {@code card}, already taken from the deck and in no one's hand.
     *
     * @param faceUp whether the card lay face up in the deck (an Imploding Kitten put back)
     */
    Outcome apply(GameContext ctx, Player actor, Card card, boolean faceUp);
}
