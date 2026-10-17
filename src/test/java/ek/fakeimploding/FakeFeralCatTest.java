package ek.fakeimploding;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import org.junit.jupiter.api.Test;

import static ek.fakeimploding.FakeCardType.FAKE_FERAL_CAT;
import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Feral Cat through combo categories: it counts as any cat in a combo, and only there. */
class FakeFeralCatTest {

    private final RecordingListener listener = new RecordingListener();

    @Test
    void feralCat_withATacoCat_isAPair_andSteals() {
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("Ann", ScriptedController.passing().play(FAKE_FERAL_CAT, TACO_CAT).targets(0),
                        FAKE_FERAL_CAT, TACO_CAT)
                .player("Bob", ScriptedController.passing(), DEFUSE)
                .deck(SKIP, EXPLODING_KITTEN)
                .startPlayer(0)
                .listener(listener);
        // Ann steals Bob's only card, his Defuse, so the kitten blows Bob up
        assertEquals(game.player(0), game.buildEngine().run());
        assertTrue(listener.eventsOf(GameEvent.MoveRejected.class).isEmpty());
        assertTrue(game.player(0).hand().contains(DEFUSE));
    }

    @Test
    void feralCat_withANonCat_isRejected() {
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("Ann", ScriptedController.passing().play(FAKE_FERAL_CAT, ATTACK), FAKE_FERAL_CAT, ATTACK)
                .player("Bob", ScriptedController.passing())
                .deck(SKIP, EXPLODING_KITTEN)
                .startPlayer(0)
                .listener(listener);
        game.buildEngine().run();

        assertEquals(1, listener.eventsOf(GameEvent.MoveRejected.class).size());
        assertTrue(game.player(0).hand().contains(FAKE_FERAL_CAT), "a rejected play keeps the cards");
    }
}