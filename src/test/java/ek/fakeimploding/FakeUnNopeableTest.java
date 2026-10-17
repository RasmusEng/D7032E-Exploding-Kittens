package ek.fakeimploding;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import org.junit.jupiter.api.Test;

import static ek.fakeimploding.FakeCardType.FAKE_KITTEN;
import static ek.fakeimploding.FakeCardType.FAKE_UNSTOPPABLE;
import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** A card type that says it can't be Noped skips the Nope window; no engine special case. */
class FakeUnNopeableTest {

    private final RecordingListener listener = new RecordingListener();

    @Test
    void unNopeableCard_skipsNopeWindow() {
        ScriptedController bob = ScriptedController.passing().nopes(true);
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("Ann", ScriptedController.passing().play(FAKE_UNSTOPPABLE), FAKE_UNSTOPPABLE)
                .player("Bob", bob, NOPE)
                .deck(EXPLODING_KITTEN)
                .startPlayer(0)
                .listener(listener);
        // Ann's card ends her turn without drawing, and Bob gets no chance to Nope it
        assertEquals(game.player(0), game.buildEngine().run());
        assertTrue(bob.nopeOffers().isEmpty());
        assertTrue(listener.eventsOf(GameEvent.PlayCancelled.class).isEmpty());
    }

    @Test
    void comboWithAnUnNopeableCard_skipsNopeWindowToo() {
        ScriptedController bob = ScriptedController.passing().nopes(true);
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("Ann", ScriptedController.passing().play(FAKE_UNSTOPPABLE, FAKE_UNSTOPPABLE).targets(0),
                        FAKE_UNSTOPPABLE, FAKE_UNSTOPPABLE)
                .player("Bob", bob, NOPE)
                .deck(TACO_CAT, EXPLODING_KITTEN)
                .startPlayer(0);
        game.buildEngine().run();

        assertTrue(bob.nopeOffers().isEmpty());
        assertTrue(game.player(0).hand().contains(NOPE), "the pair stole Bob's only card");
    }

    @Test
    void nopeableCard_stillOffersTheWindow() {
        ScriptedController bob = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("Ann", ScriptedController.passing().play(SKIP), SKIP)
                .player("Bob", bob, NOPE)
                .deck(EXPLODING_KITTEN)
                .startPlayer(0);
        game.buildEngine().run();

        assertEquals(1, bob.nopeOffers().size());
    }

    @Test
    void implodingKitten_saysItCantBeNoped() {
        assertFalse(FAKE_KITTEN.canBeNoped());
    }
}