package ek.engine;

import ek.model.Deck;
import ek.model.DiscardPile;
import ek.model.Player;

import java.util.List;

/** Everything that makes up a running game. Shared by the engine and, later, card effects. */
public final class GameState {

    private final List<Player> players;
    private final Deck deck;
    private final DiscardPile discardPile;
    private final TurnState turns = new TurnState();

    public GameState(List<Player> players, Deck deck, DiscardPile discardPile) {
        this.players = List.copyOf(players);
        this.deck = deck;
        this.discardPile = discardPile;
    }

    /** All players in seat order, including exploded ones. */
    public List<Player> players() {
        return players;
    }

    public List<Player> alivePlayers() {
        return players.stream().filter(Player::isAlive).toList();
    }

    public Player currentPlayer() {
        return players.get(turns.currentIndex());
    }

    public Deck deck() {
        return deck;
    }

    public DiscardPile discardPile() {
        return discardPile;
    }

    public TurnState turns() {
        return turns;
    }
}
