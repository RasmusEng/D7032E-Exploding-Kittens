package ek.cards;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import ek.model.Card;
import ek.player.HandChange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class FavorEffectTest {

    @Test
    void rule11b_favor_targetGivesCardOfTheirChoice() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().targets(1))           // candidates: B, C
                .player("B", ScriptedController.passing())
                .player("C", ScriptedController.passing().gives(1), TACO_CAT, NOPE)
                .listener(listener);
        new FavorEffect().apply(game.buildContext(), game.player(0));

        assertTrue(game.player(0).hand().contains(NOPE));
        assertEquals(List.of(TACO_CAT), game.player(2).hand().cards().stream().map(c -> c.type()).toList());
        assertEquals(List.of(new GameEvent.CardTaken(game.player(2), game.player(0))),
                listener.eventsOf(GameEvent.CardTaken.class));
    }

    @Test
    void favor_bothPlayersAreToldWhichCardMoved() {
        ScriptedController a = ScriptedController.passing().targets(0);
        ScriptedController b = ScriptedController.passing().gives(0);
        GameStateBuilder game = new GameStateBuilder()
                .player("A", a)
                .player("B", b, NOPE);
        new FavorEffect().apply(game.buildContext(), game.player(0));

        assertEquals(List.of(new HandChange.Received(new Card(NOPE), "B")), a.handChanges());
        assertEquals(List.of(new HandChange.Lost(new Card(NOPE), "A")), b.handChanges());
    }

    @Test
    void favor_targetWithEmptyHand_givesNothing() {
        ScriptedController b = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().targets(0))
                .player("B", b);
        new FavorEffect().apply(game.buildContext(), game.player(0));
        assertTrue(game.player(0).hand().isEmpty());
        assertEquals(0, b.giftRequests());
    }

    @Test
    void favor_invalidAnswers_askedAgain() {
        ScriptedController b = ScriptedController.passing().gives(7, -1, 0);
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().targets(5, 0))
                .player("B", b, SKIP);
        new FavorEffect().apply(game.buildContext(), game.player(0));
        assertTrue(game.player(0).hand().contains(SKIP));
        assertEquals(3, b.giftRequests());
    }

    @Test
    void favor_cannotTargetExplodedPlayer() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().targets(0))  // only candidate: C
                .player("B", ScriptedController.passing(), SKIP)
                .player("C", ScriptedController.passing().gives(0), NOPE);
        var ctx = game.buildContext();
        game.player(1).explode();
        new FavorEffect().apply(ctx, game.player(0));
        assertTrue(game.player(0).hand().contains(NOPE));
    }
}
