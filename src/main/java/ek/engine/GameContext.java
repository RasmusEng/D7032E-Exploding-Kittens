package ek.engine;

import ek.model.Card;
import ek.model.CardType;
import ek.model.Deck;
import ek.model.Player;
import ek.player.CardChoice;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.player.HandChange;
import ek.player.PlayerController;
import ek.util.Randomizer;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * What a card effect may use (Facade): the game state, the players' controllers, randomness,
 * event publishing and the draw path. Also builds each player's {@link GameView} and re-asks
 * controllers until they give a valid answer.
 */
public final class GameContext {

    private final GameState state;
    private final Map<Player, PlayerController> controllers;
    private final Map<CardType, DrawEffect> drawEffects;
    private final Randomizer randomizer;
    private final Consumer<GameEvent> publisher;

    /** @param drawEffects what drawing each card type does; any other card goes into the hand */
    public GameContext(GameState state, Map<Player, PlayerController> controllers,
                       Map<CardType, DrawEffect> drawEffects, Randomizer randomizer, Consumer<GameEvent> publisher) {
        this.state = state;
        this.controllers = Map.copyOf(controllers);
        this.drawEffects = Map.copyOf(drawEffects);
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

    /**
     * Ends a turn by drawing (rule 9a): {@code actor} draws from {@code side} of the deck,
     * the card's {@link DrawEffect} handles it (rule 10), and the turn moves on by the
     * outcome: one turn ends if they survived, all of them if they are out. Pass draws from
     * the top; a card such as Draw from the Bottom calls this with the other side.
     */
    public void drawAndEndTurn(Player actor, Deck.Side side) {
        boolean faceUp = state.deck().isFaceUp(side);
        Card card = state.deck().draw(side);
        publish(new GameEvent.CardDrawn(actor));
        DrawEffect effect = drawEffects.getOrDefault(card.type(), DrawEffect.KEEP);
        switch (effect.apply(this, actor, card, faceUp)) {
            case SURVIVED -> state.turns().endTurn(state.players());
            case EXPLODED -> state.turns().advance(state.players());
        }
    }

    /** Asks {@code actor} to pick another alive player. */
    public Player chooseTarget(Player actor) {
        List<Player> targets = state.alivePlayers().stream().filter(p -> p != actor).toList();
        List<OpponentView> candidates = targets.stream().map(GameContext::opponentView).toList();
        int index = askInRange(
                () -> controllerOf(actor).chooseTarget(viewFor(actor), candidates), 0, targets.size() - 1);
        return targets.get(index);
    }

    /** Asks {@code actor} where to put a card back into the deck: 0 (top) to the deck's size (bottom). */
    public int chooseDeckPosition(Player actor) {
        int deckSize = state.deck().size();
        PlayerController controller = controllerOf(actor);
        return askInRange(() -> controller.chooseKittenPosition(viewFor(actor), deckSize), 0, deckSize);
    }

    /** R10b: {@code actor} discards their hand and {@code kitten}, and is out of the game. */
    public void explode(Player actor, Card kitten) {
        knockOut(actor, kitten);
        publish(new GameEvent.PlayerExploded(actor));
    }

    /**
     * Like {@link #explode}, for a card other than an Exploding Kitten (an Imploding Kitten):
     * {@code actor} discards their hand and {@code cause}, is out, and everyone is told why.
     */
    public void eliminate(Player actor, Card cause) {
        knockOut(actor, cause);
        publish(new GameEvent.PlayerEliminated(actor, cause.type()));
    }

    /** Reverses the direction of play (Reverse) and tells everyone; doesn't end the turn. */
    public void reverseDirection(Player actor) {
        state.turns().reverse();
        publish(new GameEvent.DirectionReversed(actor));
    }

    /** Asks {@code actor} where to put {@code card} back, puts it there face up, and tells everyone which card. */
    public void putBackFaceUp(Player actor, Card card) {
        state.deck().insertFaceUpAt(chooseDeckPosition(actor), card);
        publish(new GameEvent.CardPutBackFaceUp(actor, card.type()));
    }

    private void knockOut(Player actor, Card cause) {
        state.discardPile().addAll(actor.explode());
        state.discardPile().add(cause);
    }

    /** Asks {@code actor} until they make a choice that {@code choice} accepts. */
    public List<Integer> chooseCards(Player actor, CardChoice choice) {
        PlayerController controller = controllerOf(actor);
        return askUntil(() -> controller.chooseCards(viewFor(actor), choice), choice::accepts);
    }

    /** Asks until the answer is in {@code min..max} (inclusive). */
    public static int askInRange(IntSupplier ask, int min, int max) {
        return askUntil(ask::getAsInt, answer -> answer >= min && answer <= max);
    }

    /** Asks until {@code valid} accepts the answer. */
    public static <T> T askUntil(Supplier<T> ask, Predicate<T> valid) {
        T answer;
        do {
            answer = ask.get();
        } while (!valid.test(answer));
        return answer;
    }

    /** What {@code viewer} is allowed to see. */
    public GameView viewFor(Player viewer) {
        List<OpponentView> opponents = state.players().stream()
                .filter(p -> p != viewer)
                .map(GameContext::opponentView)
                .toList();
        return new GameView(viewer.hand().cards(), opponents,
                state.deck().size(), state.turns().turnsLeft(), state.deck().faceUpPositions());
    }

    private static OpponentView opponentView(Player p) {
        return new OpponentView(p.name(), p.hand().size(), p.isAlive());
    }
}
