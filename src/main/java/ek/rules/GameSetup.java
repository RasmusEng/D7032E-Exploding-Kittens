package ek.rules;

import ek.expansion.DeckFactory;
import ek.model.Card;
import ek.model.Deck;
import ek.model.Player;
import ek.util.Randomizer;

import java.util.List;

import static ek.model.BaseCardType.DEFUSE;
import static ek.model.BaseCardType.EXPLODING_KITTEN;

/** Prepares hands and deck before the first turn (rules 1–7). */
public final class GameSetup {

    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 5;
    public static final int CARDS_DEALT = 7;

    private final DeckFactory deckFactory;
    private final Randomizer randomizer;

    public GameSetup(DeckFactory deckFactory, Randomizer randomizer) {
        this.deckFactory = deckFactory;
        this.randomizer = randomizer;
    }

    /**
     * Gives every player a Defuse and 7 dealt cards, and returns the shuffled draw deck
     * with the Exploding Kittens in it.
     *
     * @throws IllegalArgumentException if there are not 2–5 players
     */
    public Deck setUp(List<Player> players) {
        int playerCount = players.size();
        if (playerCount < MIN_PLAYERS || playerCount > MAX_PLAYERS) {
            throw new IllegalArgumentException(
                    "Need " + MIN_PLAYERS + "–" + MAX_PLAYERS + " players, got " + playerCount);
        }
        players.forEach(p -> p.hand().add(new Card(DEFUSE)));       // R2 every player will be delt a defuse card
        Deck deck = deckFactory.build(playerCount);                 // R3
        deck.shuffle(randomizer);                                   // R4
        deal(players, deck);                                        // R5
        for (int i = 0; i < playerCount - 1; i++) {                 // R6
            deck.addToBottom(new Card(EXPLODING_KITTEN));
        }
        deck.shuffle(randomizer);                                   // R7
        return deck;
    }

    private static void deal(List<Player> players, Deck deck) {
        for (int round = 0; round < CARDS_DEALT; round++) {
            for (Player player : players) {
                player.hand().add(deck.draw());
            }
        }
    }
}
