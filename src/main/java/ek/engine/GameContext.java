package ek.engine;

import ek.model.Card;
import ek.model.Player;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.player.HandChange;
import ek.player.PlayerController;
import ek.util.Randomizer;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * What a card effect may use: the game state, the players' controllers, randomness and
 * event publishing. Also builds each player's {@link GameView} and re-asks controllers
 * until they give a valid answer.
 */
public final class GameContext {

    private final GameState state;
    private final Map<Player, PlayerController> controllers;
    private final Randomizer randomizer;
    private final Consumer<GameEvent> publisher;

    public GameContext(GameState state, Map<Player, PlayerController> controllers,
                       Randomizer randomizer, Consumer<GameEvent> publisher) {
        this.state = state;
        this.controllers = Map.copyOf(controllers);
        this.randomizer = randomizer;
        this.publisher = publisher;
    }

    public GameState state() {
        return state;
    }

    public Randomizer randomizer() {
        return randomizer;
    }

    public PlayerController controllerOf(Player player) {
        return controllers.get(player);
    }

    public void publish(GameEvent event) {
        publisher.accept(event);
    }

    /**
     * Moves {@code card}, already removed from {@code from}'s hand, to {@code to}'s hand.
     * Everyone is told that a card moved; only the two players are told which.
     */
    public void giveCard(Card card, Player from, Player to) {
        to.hand().add(card);
        controllerOf(from).handChanged(new HandChange.Lost(card, to.name()));
        controllerOf(to).handChanged(new HandChange.Received(card, from.name()));
        publish(new GameEvent.CardTaken(from, to));
    }

    /** Asks {@code actor} to pick another alive player. */
    public Player chooseTarget(Player actor) {
        List<Player> targets = state.alivePlayers().stream().filter(p -> p != actor).toList();
        List<OpponentView> candidates = targets.stream().map(GameContext::opponentView).toList();
        int index = askInRange(
                () -> controllerOf(actor).chooseTarget(viewFor(actor), candidates), 0, targets.size() - 1);
        return targets.get(index);
    }

    /** Asks until the answer is in {@code min..max} (inclusive). */
    public static int askInRange(IntSupplier ask, int min, int max) {
        int answer;
        do {
            answer = ask.getAsInt();
        } while (answer < min || answer > max);
        return answer;
    }

    /** What {@code viewer} is allowed to see. */
    public GameView viewFor(Player viewer) {
        List<OpponentView> opponents = state.players().stream()
                .filter(p -> p != viewer)
                .map(GameContext::opponentView)
                .toList();
        return new GameView(viewer.hand().cards(), opponents,
                state.deck().size(), state.turns().turnsLeft());
    }

    private static OpponentView opponentView(Player p) {
        return new OpponentView(p.name(), p.hand().size(), p.isAlive());
    }
}
