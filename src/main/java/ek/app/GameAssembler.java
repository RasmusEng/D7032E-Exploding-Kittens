package ek.app;

import ek.combos.ThreeOfAKind;
import ek.combos.TwoOfAKind;
import ek.engine.CardEffect;
import ek.engine.DrawEffect;
import ek.engine.GameEngine;
import ek.engine.GameEventListener;
import ek.engine.GameState;
import ek.engine.NopeResolver;
import ek.engine.PlayResolver;
import ek.expansion.DeckFactory;
import ek.model.CardType;
import ek.model.Deck;
import ek.model.DiscardPile;
import ek.model.Player;
import ek.player.PlayerController;
import ek.rules.GameConfig;
import ek.rules.GameSetup;
import ek.util.Randomizer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

/**
 * Builds the game for a {@link GameConfig}: collects the chosen expansions' card and draw
 * effects, builds the deck with the chosen variants, and creates the engine. It does no I/O
 * and opens no sockets, so the whole assembly is testable; {@link Main} only creates the
 * players' controllers and runs the result.
 */
public final class GameAssembler {

    private final GameConfig config;
    private final Randomizer randomizer;
    private final Map<CardType, CardEffect> effects = new LinkedHashMap<>();
    private final Map<CardType, DrawEffect> drawEffects = new LinkedHashMap<>();
    private final DeckFactory deckFactory;
    private final List<CardType> nameableCards;

    public GameAssembler(GameConfig config, Randomizer randomizer) {
        this.config = config;
        this.randomizer = randomizer;
        config.expansions().forEach(e -> effects.putAll(e.cardEffects()));
        config.expansions().forEach(e -> drawEffects.putAll(e.drawEffects()));
        deckFactory = new DeckFactory(config.expansions());
        nameableCards = List.copyOf(deckFactory.cardTypes(deckFactory.playerRange().max()));
    }

    /** Every card type that can be in the game, offered when naming a card for three of a kind. */
    public List<CardType> nameableCards() {
        return nameableCards;
    }

    /** The card types that do something when played alone, for bots to choose from. */
    public Set<CardType> playableCards() {
        return Set.copyOf(effects.keySet());
    }

    /**
     * The play rules: every chosen expansion's card effects plus both combos. Expansions are
     * applied in {@link ek.expansion.GameOption#BY_ORDER} (base game first), so a later
     * expansion's effect for a card type replaces an earlier one's. That is an extension point.
     */
    PlayResolver playResolver() {
        return new PlayResolver(effects, new TwoOfAKind(), new ThreeOfAKind());
    }

    /** What drawing each card type does, overridden in the same order as {@link #playResolver()}. */
    Map<CardType, DrawEffect> drawEffects() {
        return Map.copyOf(drawEffects);
    }

    /**
     * Deals the players' hands and returns the draw deck (rules 1–7, with the variants).
     *
     * @throws IllegalArgumentException if the number of players is outside the allowed range
     */
    public Deck setUp(List<Player> players) {
        return new GameSetup(deckFactory, randomizer, config.variants()).setUp(players);
    }

    /**
     * Sets the game up for the controllers' players, in their iteration order (the seat
     * order), and returns an engine ready to run.
     *
     * @param nopeExecutor runs the Nope offers; they block, so it needs a thread per task
     */
    public GameEngine assemble(Map<Player, PlayerController> controllers, List<GameEventListener> listeners,
                               Executor nopeExecutor) {
        List<Player> players = new ArrayList<>(controllers.keySet());
        Deck deck = setUp(players);
        return new GameEngine(
                new GameState(players, deck, new DiscardPile()),
                controllers,
                playResolver(),
                drawEffects(),
                new NopeResolver(NopeResolver.DEFAULT_WINDOW, nopeExecutor),
                randomizer,
                listeners);
    }
}
