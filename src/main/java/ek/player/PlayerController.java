package ek.player;

import ek.model.Card;
import ek.model.CardType;
import ek.player.GameView.OpponentView;

import java.util.List;

/**
 * Makes a player's decisions. The engine talks to every player through this interface,
 * so console, remote and bot players are interchangeable.
 *
 * <p>Methods returning an index are asked again by the engine if the answer is out of
 * range. Implementations may block while waiting for input.
 */
public interface PlayerController {

    /** Chooses what to do next on this turn: pass, or play cards. */
    TurnAction chooseAction(GameView view);

    /**
     * Chooses where to put a defused Exploding Kitten back into the deck.
     *
     * @param deckSize cards in the deck now
     * @return a position from 0 (top) to {@code deckSize} (bottom)
     */
    int chooseKittenPosition(GameView view, int deckSize);

    /**
     * Chooses which player to take a card from (Favor, two/three of a kind).
     *
     * @return an index into {@code candidates}
     */
    int chooseTarget(GameView view, List<OpponentView> candidates);

    /**
     * Chooses which of your own cards to give away when someone plays Favor on you.
     *
     * @param requester name of the player who gets the card
     * @return an index into {@code view.hand()}
     */
    int chooseCardToGive(GameView view, String requester);

    /** Names the card type to ask for with three of a kind. */
    CardType nameCard(GameView view);

    /** Shows the top cards of the deck, top first (See The Future); private to this player. */
    void seeTheFuture(List<Card> topCards);

    /**
     * Offers the chance to play Nope (only to players holding one). May run on a separate
     * thread. To Nope, call {@link NopeOffer#playNope()} — now or later from another thread,
     * while the window is open. Doing nothing means no Nope.
     */
    void offerNope(GameView view, NopeOffer offer);

    /**
     * Tells this player privately which card entered or left their hand. Purely
     * informational, so the default ignores it: a controller that keeps no memory
     * (like a random bot) needs no change.
     */
    default void handChanged(HandChange change) {
    }
}
