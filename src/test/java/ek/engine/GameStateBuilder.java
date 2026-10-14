package ek.engine;

import ek.combos.ThreeOfAKind;
import ek.combos.TwoOfAKind;
import ek.expansion.BaseGame;
import ek.model.Card;
import ek.model.CardType;
import ek.model.Deck;
import ek.model.DiscardPile;
import ek.model.Player;
import ek.player.PlayerController;
import ek.util.Randomizer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds a game in an exact state: given hands, deck order and start player.
 * Either run it ({@link #buildEngine()}) or get a context to apply one effect ({@link #buildContext()}).
 */
public final class GameStateBuilder {

    private final List<Player> players = new ArrayList<>();
    private final Map<Player, PlayerController> controllers = new LinkedHashMap<>();
    private final List<GameEventListener> listeners = new ArrayList<>();
    private final Deck deck = new Deck();
    private Randomizer randomizer = bound -> 0;
    private Integer startIndex;
    private NopeResolver nopeResolver = instantNopeResolver();
    private Map<CardType, DrawEffect> drawEffects = baseDrawEffects();
    private GameState state;

    /** The base game's play rules: base card effects plus both combos. */
    public static PlayResolver basePlayResolver() {
        return new PlayResolver(new BaseGame().cardEffects(), new TwoOfAKind(), new ThreeOfAKind());
    }

    /** The base game's draw rules: a drawn Exploding Kitten is defused or explodes. */
    public static Map<CardType, DrawEffect> baseDrawEffects() {
        return new BaseGame().drawEffects();
    }

    /** Nope window of 0 s run on the calling thread: only Nopes played during the offer count. */
    public static NopeResolver instantNopeResolver() {
        return new NopeResolver(Duration.ZERO, Runnable::run);
    }

    public GameStateBuilder nopeResolver(NopeResolver nopeResolver) {
        this.nopeResolver = nopeResolver;
        return this;
    }

    /** What drawing each card type does, instead of the base game's. */
    public GameStateBuilder drawEffects(Map<CardType, DrawEffect> drawEffects) {
        this.drawEffects = drawEffects;
        return this;
    }

    /** Adds a player with the given controller and hand. */
    public GameStateBuilder player(String name, PlayerController controller, CardType... hand) {
        Player player = new Player(name);
        for (CardType type : hand) {
            player.hand().add(new Card(type));
        }
        players.add(player);
        controllers.put(player, controller);
        return this;
    }

    /** Sets the deck, top card first. */
    public GameStateBuilder deck(CardType... topFirst) {
        for (CardType type : topFirst) {
            deck.addToBottom(new Card(type));
        }
        return this;
    }

    /** The player at this seat goes first; the randomizer is then only used after setup. */
    public GameStateBuilder startPlayer(int index) {
        startIndex = index;
        return this;
    }

    /** Randomness for the engine and effects. Without {@link #startPlayer}, it also picks who starts. */
    public GameStateBuilder randomizer(Randomizer randomizer) {
        this.randomizer = randomizer;
        return this;
    }

    public GameStateBuilder listener(GameEventListener listener) {
        listeners.add(listener);
        return this;
    }

    public GameEngine buildEngine() {
        state = new GameState(players, deck, new DiscardPile());
        Randomizer engineRandomizer = startIndex == null ? randomizer : startFirst(startIndex, randomizer);
        return new GameEngine(state, controllers, basePlayResolver(), drawEffects, nopeResolver,
                engineRandomizer, listeners);
    }

    /** A context for applying a single effect; the start player's turn has begun. */
    public GameContext buildContext() {
        state = new GameState(players, deck, new DiscardPile());
        state.turns().startAt(startIndex == null ? 0 : startIndex);
        return new GameContext(state, controllers, randomizer,
                event -> listeners.forEach(l -> l.onEvent(event)));
    }

    /** The state behind the last built engine or context. */
    public GameState state() {
        return state;
    }

    public Player player(int index) {
        return players.get(index);
    }

    /** Returns {@code first} on the first call (start player), then delegates. */
    private static Randomizer startFirst(int first, Randomizer then) {
        boolean[] used = {false};
        return bound -> {
            if (!used[0]) {
                used[0] = true;
                return first;
            }
            return then.nextInt(bound);
        };
    }
}
