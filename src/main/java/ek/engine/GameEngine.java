package ek.engine;

import ek.model.Card;
import ek.model.CardType;
import ek.model.Player;
import ek.player.HandChange;
import ek.player.PlayerController;
import ek.player.TurnAction;
import ek.rules.TurnState;
import ek.util.Randomizer;

import java.util.List;
import java.util.Map;

import static ek.model.BaseCardType.DEFUSE;
import static ek.model.BaseCardType.EXPLODING_KITTEN;

/** Runs a set-up game from the first turn until one player is left. */
public final class GameEngine {

    private final GameState state;
    private final GameContext context;
    private final PlayResolver playResolver;
    private final NopeResolver nopeResolver;
    private final Randomizer randomizer;
    private final List<GameEventListener> listeners;

    public GameEngine(GameState state, Map<Player, PlayerController> controllers,
                      PlayResolver playResolver, NopeResolver nopeResolver,
                      Randomizer randomizer, List<GameEventListener> listeners) {
        this.state = state;
        this.playResolver = playResolver;
        this.nopeResolver = nopeResolver;
        this.randomizer = randomizer;
        this.listeners = List.copyOf(listeners);
        this.context = new GameContext(state, controllers, randomizer, this::publish);
    }

    /** Plays the game to the end and returns the winner. */
    public Player run() {
        state.turns().startAt(randomizer.nextInt(state.players().size()));    // R8
        while (state.alivePlayers().size() > 1) {
            playTurn();
        }
        Player winner = state.alivePlayers().get(0);
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
                case TurnAction.Pass pass -> drawAndEndTurn(actor);            // R9a
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
        if (nopeResolver.isCancelled(context, actor, cards)) {                 // R12
            publish(new GameEvent.PlayCancelled(actor, cards));
            return;
        }
        effect.apply(context, actor);                                           // R11
    }

    private void drawAndEndTurn(Player actor) {
        TurnState turns = state.turns();
        Card card = state.deck().draw();
        publish(new GameEvent.CardDrawn(actor));
        if (!card.type().equals(EXPLODING_KITTEN)) {
            actor.hand().add(card);
            context.controllerOf(actor).handChanged(new HandChange.Drawn(card));
            turns.endTurn(state.players());
        } else if (actor.hand().contains(DEFUSE)) {
            defuse(actor, card);
            turns.endTurn(state.players());
        } else {
            explode(actor, card);
            turns.advance(state.players());
        }
    }

    /** R10a: discard the Defuse and put the kitten back where the player chooses. */
    private void defuse(Player actor, Card kitten) {
        state.discardPile().add(actor.hand().remove(DEFUSE));
        int deckSize = state.deck().size();
        PlayerController controller = context.controllerOf(actor);
        int position = GameContext.askInRange(
                () -> controller.chooseKittenPosition(context.viewFor(actor), deckSize), 0, deckSize);
        state.deck().insertAt(position, kitten);
        publish(new GameEvent.KittenDefused(actor));
    }

    /** R10b: the player discards their hand and the kitten, and is out of the game. */
    private void explode(Player actor, Card kitten) {
        state.discardPile().addAll(actor.explode());
        state.discardPile().add(kitten);
        publish(new GameEvent.PlayerExploded(actor));
    }

    private void publish(GameEvent event) {
        listeners.forEach(l -> l.onEvent(event));
    }
}
