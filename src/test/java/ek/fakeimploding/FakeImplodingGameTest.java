package ek.fakeimploding;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.fakeimploding.FakeCardType.*;
import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A whole scripted game of the base game plus the fake expansion, through the real engine.
 * Every Imploding Kittens hook is used, and the fake needs no change to production code.
 */
class FakeImplodingGameTest {

    @Test
    void scriptedGame_baseAndFake_usesEveryHook() {
        RecordingListener listener = new RecordingListener();
        ScriptedController ann = ScriptedController.passing(0)               // defused kitten back on top
                .play(FAKE_REVERSE)                                          // 1: play goes the other way
                .play(FAKE_FERAL_CAT, TACO_CAT).targets(1)                   // 4: wildcard pair, steal from Cid
                .pass()                                                      //    first of 2 turns ends
                .play(FAKE_DRAW_FROM_THE_BOTTOM)                             // 5: bottom card is a kitten: defuse
                .play(FAKE_UNSTOPPABLE);                                     // 8: no Nope window
        ScriptedController bob = ScriptedController.passing(0)               // face-up kitten back on top
                .play(FAKE_TARGETED_ATTACK).targets(0);                      // 3: Ann takes 2 turns
        ScriptedController cid = ScriptedController.passing()
                .play(FAKE_ALTER_THE_FUTURE).chooses(List.of(1, 2, 0));      // 2: kitten moves 3rd from top
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("Ann", ann, FAKE_REVERSE, FAKE_FERAL_CAT, TACO_CAT, FAKE_DRAW_FROM_THE_BOTTOM, DEFUSE,
                        FAKE_UNSTOPPABLE)
                .player("Bob", bob, FAKE_TARGETED_ATTACK)
                .player("Cid", cid, FAKE_ALTER_THE_FUTURE)
                .deck(FAKE_KITTEN, SKIP, TACO_CAT, EXPLODING_KITTEN)
                .startPlayer(0)
                .listener(listener);

        assertEquals(game.player(0), game.buildEngine().run());

        // reversed, then targeted: C, B, A twice, then on from A the other way round
        assertEquals(List.of("Ann:1", "Cid:1", "Bob:1", "Ann:2", "Ann:1", "Cid:1", "Bob:1", "Ann:1", "Bob:1"),
                listener.eventsOf(GameEvent.TurnStarted.class).stream()
                        .map(e -> e.player() + ":" + e.turnsLeft()).toList());
        assertEquals(List.of(new GameEvent.DirectionReversed("Ann")), listener.eventsOf(GameEvent.DirectionReversed.class));
        assertEquals(List.of(new GameEvent.CardTaken("Cid", "Ann")), listener.eventsOf(GameEvent.CardTaken.class));
        assertEquals(List.of(new GameEvent.KittenDefused("Ann")), listener.eventsOf(GameEvent.KittenDefused.class));
        assertEquals(List.of(new GameEvent.PlayerExploded("Cid")), listener.eventsOf(GameEvent.PlayerExploded.class));
        assertEquals(List.of(new GameEvent.CardPutBackFaceUp("Bob", FAKE_KITTEN)),
                listener.eventsOf(GameEvent.CardPutBackFaceUp.class));
        assertEquals(List.of(new GameEvent.PlayerEliminated("Bob", FAKE_KITTEN)),
                listener.eventsOf(GameEvent.PlayerEliminated.class));
        assertTrue(listener.eventsOf(GameEvent.MoveRejected.class).isEmpty());
        // Bob was offered a Nope window on Reverse, Alter the Future, the pair and Draw from the Bottom,
        // but not on the un-Nopeable card
        assertEquals(4, bob.nopeOffers().size());
        assertEquals(List.of(new GameEvent.Winner("Ann")), listener.eventsOf(GameEvent.Winner.class));
    }
}