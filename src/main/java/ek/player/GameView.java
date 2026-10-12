package ek.player;

import ek.model.Card;

import java.io.Serializable;
import java.util.List;

/**
 * Read-only snapshot of the game as one player may see it. Controllers get this instead
 * of the game state, so they cannot change the game or see other players' cards.
 *
 * @param hand      the viewing player's own cards
 * @param opponents every other player, in seat order
 * @param deckSize  cards left in the draw deck
 * @param turnsLeft turns the current player still has to take
 */
public record GameView(List<Card> hand, List<OpponentView> opponents, int deckSize, int turnsLeft)
        implements Serializable {

    public GameView {
        hand = List.copyOf(hand);
        opponents = List.copyOf(opponents);
    }

    /** What is publicly known about another player. */
    public record OpponentView(String name, int handSize, boolean alive) implements Serializable {
    }
}
