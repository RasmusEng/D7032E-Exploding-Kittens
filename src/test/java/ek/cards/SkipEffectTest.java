package ek.cards;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class SkipEffectTest {

    @Test
    void rule11d_skip_endsTurnWithoutDrawing() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().play(SKIP), SKIP)
                .player("B", ScriptedController.passing())
                .deck(EXPLODING_KITTEN)
                .listener(listener);
        // A skips instead of drawing, so B draws the kitten
        assertEquals(game.player(0), game.buildEngine().run());
        assertEquals(1, listener.eventsOf(GameEvent.CardDrawn.class).size());
    }

    @Test
    void rule11d_skipUnderAttack_endsOnlyOneTurn() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().play(ATTACK), ATTACK)
                .player("B", ScriptedController.passing().play(SKIP), SKIP)
                .deck(EXPLODING_KITTEN)
                .listener(listener);
        // B has 2 turns: Skip ends the first, B must still take the second and draws the kitten
        assertEquals(game.player(0), game.buildEngine().run());

        var b = game.player(1);
        assertEquals(List.of(new GameEvent.TurnStarted(b, 2), new GameEvent.TurnStarted(b, 1)),
                listener.eventsOf(GameEvent.TurnStarted.class).subList(1, 3));
    }

    @Test
    void twoSkipsUnderAttack_endBothTurns() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", ScriptedController.passing())
                .player("C", ScriptedController.passing());
        var ctx = game.buildContext();
        new AttackEffect().apply(ctx, game.player(0));
        new SkipEffect().apply(ctx, game.player(1));
        new SkipEffect().apply(ctx, game.player(1));
        assertEquals(2, game.state().turns().currentIndex());
        assertEquals(1, game.state().turns().turnsLeft());
    }
}
