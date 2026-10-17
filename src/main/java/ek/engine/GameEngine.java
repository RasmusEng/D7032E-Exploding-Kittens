package ek.engine;

import ek.model.CardType;
import ek.model.Deck;
import ek.model.Player;
import ek.player.PlayerController;
import ek.player.TurnAction;
import ek.util.Randomizer;

import java.util.List;
import java.util.Map;

/** Runs a set-up game from the first turn until one player is left. */
public final class GameEngine {

    private final GameState state;
    private final GameContext context;
    private final PlayResolver playResolver;
    private final NopeResolver nopeResolver;
    private final Randomizer randomizer;
    private final List<GameEventListener> listeners;

    /** @param drawEffects what drawing each card type does; any other card goes into the hand */
    public GameEngine(GameState state, Map<Player, PlayerController> controllers,
                      PlayResolver playResolver, Map<CardType, DrawEffect> drawEffects,
                      NopeResolver nopeResolver, Randomizer randomizer, List<GameEventListener> listeners) {
        this.state = state;
        this.playResolver = playResolver;
        this.nopeResolver = nopeResolver;
        this.randomizer = randomizer;
        this.listeners = List.copyOf(listeners);
        this.context = new GameContext(state, controllers, drawEffects, randomizer, this::publish);
    }

    /** Plays the game to the end and returns the winner. */
    public Player run() {
        state.turns().startAt(randomizer.nextInt(state.players().size()));    // R8
        for (Player player : state.players()) {
            context.controllerOf(player).gameStarted(context.viewFor(player));
        }
        while (state.alivePlayers().size() > 1) {
            playTurn();
        }
        Player winner = state.alivePlayers().getFirst();
        publish(new GameEvent.Winner(winner));                                  // R13
        return winner;
    }

    /** One turn: the player plays any number of cards until they pass or a card ends the turn. */
    private void playTurn() {
        Player actor = state.currentPlayer();
        int turnId = state.turns().turnId();
        publish(new GameEvent.TurnStarted(actor, state.turns().turnsLeft()));
        while (state.turns().turnId() == turnId) {
            TurnAction action = context.controllerOf(actor).chooseAction(context.viewFor(actor));
            switch (action) {
                case TurnAction.Pass pass -> context.drawAndEndTurn(actor, Deck.Side.TOP);   // R9a, R10
                case TurnAction.Play play -> play(actor, play.cards());         // R9b
            }
        }
    }

    private void play(Player actor, List<CardType> cards) {
        CardEffect effect;
        try {
            effect = playResolver.resolve(cards, actor.hand());
        } catch (InvalidMoveException e) {
            publish(new GameEvent.MoveRejected(actor, e.getMessage()));
            return;
        }
        cards.forEach(type -> state.discardPile().add(actor.hand().remove(type)));
        publish(new GameEvent.CardsPlayed(actor, cards));
        if (canBeNoped(cards) && nopeResolver.isCancelled(context, actor, cards)) {     // R12
            publish(new GameEvent.PlayCancelled(actor, cards));
            return;
        }
        effect.apply(context, actor);                                           // R11
    }

    /** A play is one action: one card that can't be Noped makes the whole play unstoppable. */
    private static boolean canBeNoped(List<CardType> cards) {
        return cards.stream().allMatch(CardType::canBeNoped);
    }

    private void publish(GameEvent event) {
        listeners.forEach(l -> l.onEvent(event));
    }
}
