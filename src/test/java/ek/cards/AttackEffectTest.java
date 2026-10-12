package ek.cards;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class AttackEffectTest {

    @Test
    void rule11a_attack_endsTurnWithoutDrawing_nextPlayerTakesTwoTurns() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().play(ATTACK), ATTACK)
                .player("B", ScriptedController.passing())
                .deck(SKIP, SKIP, EXPLODING_KITTEN)
                .listener(listener);
        // A attacks; B passes twice (draws both SKIPs); A draws the kitten
        assertEquals(game.player(1), game.buildEngine().run());

        var a = game.player(0);
        var b = game.player(1);
        assertEquals(List.of(
                new GameEvent.TurnStarted(a, 1), new GameEvent.TurnStarted(b, 2),
                new GameEvent.TurnStarted(b, 1), new GameEvent.TurnStarted(a, 1)),
                listener.eventsOf(GameEvent.TurnStarted.class));
        assertEquals(2, b.hand().count(SKIP));
    }

    @Test
    void rule11a_attackedPlayerAttacks_nextPlayerTakesFourTurns() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", ScriptedController.passing())
                .player("C", ScriptedController.passing());
        var ctx = game.buildContext();
        var attack = new AttackEffect();

        attack.apply(ctx, game.player(0));              // B: 2 turns
        attack.apply(ctx, game.player(1));              // C: 2 + 2
        assertEquals(2, game.state().turns().currentIndex());
        assertEquals(4, game.state().turns().turnsLeft());

        attack.apply(ctx, game.player(2));              // A: 4 + 2
        assertEquals(0, game.state().turns().currentIndex());
        assertEquals(6, game.state().turns().turnsLeft());
    }

    @Test
    void attack_skipsExplodedPlayer() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", ScriptedController.passing())
                .player("C", ScriptedController.passing());
        var ctx = game.buildContext();
        game.player(1).explode();

        new AttackEffect().apply(ctx, game.player(0));
        assertEquals(2, game.state().turns().currentIndex());
        assertEquals(2, game.state().turns().turnsLeft());
    }
}
