package ek.fakeimploding;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.fakeimploding.FakeCardType.FAKE_TARGETED_ATTACK;
import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Targeted Attack through the generalized {@code TurnState.attack(players, victim)}. */
class FakeTargetedAttackTest {

    private final RecordingListener listener = new RecordingListener();

    private List<String> turnsStarted() {
        return listener.eventsOf(GameEvent.TurnStarted.class).stream()
                .map(e -> e.player() + ":" + e.turnsLeft()).toList();
    }

    @Test
    void targetedAttack_victimTakesTwoTurns_playContinuesFromThem() {
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                // targets are the other alive players in seat order: [B, C]
                .player("A", ScriptedController.passing().play(FAKE_TARGETED_ATTACK).targets(1), FAKE_TARGETED_ATTACK)
                .player("B", ScriptedController.passing())
                .player("C", ScriptedController.passing())
                .deck(TACO_CAT, TACO_CAT, EXPLODING_KITTEN, EXPLODING_KITTEN)
                .listener(listener);
        // C takes 2 turns, then play goes on from C: A explodes, then B
        assertEquals(game.player(2), game.buildEngine().run());
        assertEquals(List.of("A:1", "C:2", "C:1", "A:1", "B:1"), turnsStarted());
    }

    @Test
    void targetedAttack_stacksLikeAttack() {
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("A", ScriptedController.passing().play(ATTACK), ATTACK)
                // B's targets: [A, C]
                .player("B", ScriptedController.passing().play(FAKE_TARGETED_ATTACK).targets(0), FAKE_TARGETED_ATTACK)
                .player("C", ScriptedController.passing())
                .deck(TACO_CAT, TACO_CAT, TACO_CAT, TACO_CAT, EXPLODING_KITTEN, EXPLODING_KITTEN)
                .listener(listener);
        // B, attacked with 2 turns, passes them plus 2 on to A
        assertEquals(game.player(0), game.buildEngine().run());
        assertEquals(List.of("A:1", "B:2", "A:4", "A:3", "A:2", "A:1", "B:1", "C:1"), turnsStarted());
    }
}