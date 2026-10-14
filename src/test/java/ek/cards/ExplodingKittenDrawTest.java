package ek.cards;

import ek.engine.DrawEffect.Outcome;
import ek.engine.GameContext;
import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import ek.model.Card;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class ExplodingKittenDrawTest {

    private final ExplodingKittenDraw draw = new ExplodingKittenDraw();

    @Test
    void rule10a_withDefuse_survives_kittenAtChosenPosition() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing(1), DEFUSE, SKIP)
                .player("B", ScriptedController.passing())
                .deck(ATTACK, NOPE)
                .listener(listener);
        GameContext ctx = game.buildContext();

        assertEquals(Outcome.SURVIVED, draw.apply(ctx, game.player(0), new Card(EXPLODING_KITTEN)));
        assertEquals(List.of(ATTACK, EXPLODING_KITTEN, NOPE),
                game.state().deck().peek(3).stream().map(Card::type).toList());
        assertEquals(List.of(new Card(SKIP)), game.player(0).hand().cards());
        assertEquals(1, game.state().discardPile().size());
        assertEquals(List.of(new GameEvent.KittenDefused(game.player(0))),
                listener.eventsOf(GameEvent.KittenDefused.class));
    }

    @Test
    void rule10b_withoutDefuse_explodes_handAndKittenDiscarded() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing(), SKIP)
                .player("B", ScriptedController.passing());
        GameContext ctx = game.buildContext();

        assertEquals(Outcome.EXPLODED, draw.apply(ctx, game.player(0), new Card(EXPLODING_KITTEN)));
        assertFalse(game.player(0).isAlive());
        assertEquals(2, game.state().discardPile().size());
    }
}
