package ek.fakeimploding;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import ek.model.Card;
import ek.player.HandChange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.fakeimploding.FakeCardType.FAKE_DRAW_FROM_THE_BOTTOM;
import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Draw from the Bottom through the shared draw path, {@code GameContext.drawAndEndTurn}. */
class FakeDrawFromTheBottomTest {

    private final RecordingListener listener = new RecordingListener();

    @Test
    void drawFromTheBottom_bottomCardGoesToHand_andTheTurnEnds() {
        ScriptedController ann = ScriptedController.passing().play(FAKE_DRAW_FROM_THE_BOTTOM);
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("Ann", ann, FAKE_DRAW_FROM_THE_BOTTOM)
                .player("Bob", ScriptedController.passing())
                .deck(EXPLODING_KITTEN, TACO_CAT)
                .listener(listener);
        // Ann takes the Taco Cat from the bottom; Bob then draws the kitten from the top
        assertEquals(game.player(0), game.buildEngine().run());
        assertEquals(List.of(new HandChange.Drawn(new Card(TACO_CAT))), ann.handChanges());
        assertEquals(2, listener.eventsOf(GameEvent.CardDrawn.class).size());
    }

    @Test
    void drawFromTheBottom_bottomKitten_goesThroughItsDrawEffect() {
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                // Ann defuses the kitten from the bottom and puts it on top
                .player("Ann", ScriptedController.passing(0).play(FAKE_DRAW_FROM_THE_BOTTOM),
                        FAKE_DRAW_FROM_THE_BOTTOM, DEFUSE)
                .player("Bob", ScriptedController.passing())
                .deck(TACO_CAT, EXPLODING_KITTEN)
                .listener(listener);

        assertEquals(game.player(0), game.buildEngine().run());
        assertEquals(List.of(new GameEvent.KittenDefused("Ann")), listener.eventsOf(GameEvent.KittenDefused.class));
        assertEquals(List.of(new GameEvent.PlayerExploded("Bob")), listener.eventsOf(GameEvent.PlayerExploded.class));
        assertFalse(game.player(0).hand().contains(TACO_CAT));
    }
}