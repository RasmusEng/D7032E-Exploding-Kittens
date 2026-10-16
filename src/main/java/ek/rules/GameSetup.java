package ek.rules;

import ek.expansion.DeckFactory;
import ek.expansion.GameVariant;
import ek.expansion.PlayerRange;
import ek.model.Card;
import ek.model.Deck;
import ek.model.Player;
import ek.util.Randomizer;

import java.util.List;

import static ek.model.BaseCardType.DEFUSE;

/** Prepares hands and deck before the first turn (rules 1–7). */
public final class GameSetup {

    public static final int CARDS_DEALT = 7;

    private final DeckFactory deckFactory;
    private final Randomizer randomizer;
    private final List<GameVariant> variants;

    /** A setup without variants. */
    public GameSetup(DeckFactory deckFactory, Randomizer randomizer) {
        this(deckFactory, randomizer, List.of());
    }

    /** @param variants applied in this order after dealing, before the kittens go in */
    public GameSetup(DeckFactory deckFactory, Randomizer randomizer, List<GameVariant> variants) {
        this.deckFactory = deckFactory;
        this.randomizer = randomizer;
        this.variants = List.copyOf(variants);
    }

    /**
     * Gives every player a Defuse and 7 dealt cards, and returns the shuffled draw deck
     * with the kittens in it (which kittens, and how many, the expansions decide). The
     * variants may change the deck between dealing and the kittens.
     *
     * @throws IllegalArgumentException if the player count is outside the expansions'
     *                                  {@link PlayerRange} (rule 1: 2–5 for the base game)
     */
    public Deck setUp(List<Player> players) {
        int playerCount = players.size();
        PlayerRange range = deckFactory.playerRange();
        if (!range.contains(playerCount)) {
            throw new IllegalArgumentException("Need " + range + " players, got " + playerCount);
        }
        players.forEach(p -> p.hand().add(new Card(DEFUSE))); // R2 every player will be delt a defuse card
        Deck deck = deckFactory.build(playerCount);                 // R3
        deck.shuffle(randomizer);                                   // R4
        deal(players, deck);                                        // R5
        variants.forEach(v -> v.afterDeal(deck, randomizer));       // variants' one hook
        deckFactory.addAfterDeal(deck, playerCount);                // R6
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
