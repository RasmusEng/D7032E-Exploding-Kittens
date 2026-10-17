package ek.fakeimploding;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import ek.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.fakeimploding.FakeCardType.FAKE_REVERSE;
import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Reverse through the turn direction: it reverses play and ends one turn. */
class FakeReverseTest {

    private final RecordingListener listener = new RecordingListener();

    private List<String> turnsStarted() {
        return listener.eventsOf(GameEvent.TurnStarted.class).stream()
                .map(e -> e.player() + ":" + e.turnsLeft()).toList();
    }

    @Test
    void reverse_twoPlayers_actsLikeSkip() {
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("A", ScriptedController.passing().play(FAKE_REVERSE), FAKE_REVERSE)
                .player("B", ScriptedController.passing())
                .deck(EXPLODING_KITTEN)
                .listener(listener);
        // like SkipEffectTest: A ends the turn without drawing, so B draws the kitten
        assertEquals(game.player(0), game.buildEngine().run());
        assertEquals(1, listener.eventsOf(GameEvent.CardDrawn.class).size());
        assertEquals(List.of(new GameEvent.DirectionReversed("A")), listener.eventsOf(GameEvent.DirectionReversed.class));
    }

    @Test
    void reverse_threePlayers_playGoesCounterClockwise() {
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("A", ScriptedController.passing().play(FAKE_REVERSE), FAKE_REVERSE)
                .player("B", ScriptedController.passing())
                .player("C", ScriptedController.passing())
                .deck(EXPLODING_KITTEN, TACO_CAT, EXPLODING_KITTEN)
                .listener(listener);
        // A reverses: C explodes, then B draws safely, then A explodes
        Player winner = game.buildEngine().run();

        assertEquals(game.player(1), winner);
        assertEquals(List.of("A:1", "C:1", "B:1", "A:1"), turnsStarted());
    }

    @Test
    void reverse_underAttack_endsOnlyOneTurn() {
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("A", ScriptedController.passing().play(ATTACK), ATTACK)
                .player("B", ScriptedController.passing().play(FAKE_REVERSE), FAKE_REVERSE)
                .player("C", ScriptedController.passing())
                .deck(TACO_CAT, EXPLODING_KITTEN, EXPLODING_KITTEN)
                .listener(listener);
        // B reverses on the first of 2 turns, takes the second, then play goes back to A
        Player winner = game.buildEngine().run();

        assertEquals(game.player(1), winner);
        assertEquals(List.of("A:1", "B:2", "B:1", "A:1", "C:1"), turnsStarted());
    }
}