package ek.fakeimploding;

import ek.engine.GameContext;
import ek.engine.GameStateBuilder;
import ek.engine.ScriptedController;
import ek.model.Card;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Alter the Future through the generic card choice: no PlayerController method of its own. */
class FakeAlterTheFutureTest {

    @Test
    void alterTheFuture_playerReordersTopThree() {
        ScriptedController ann = ScriptedController.passing().chooses(List.of(2, 0, 1));
        GameStateBuilder game = new GameStateBuilder()
                .player("Ann", ann).player("Bob", ScriptedController.passing())
                .deck(SKIP, ATTACK, NOPE, TACO_CAT);
        GameContext ctx = game.buildContext();

        new FakeAlterTheFuture().apply(ctx, game.player(0));

        assertEquals(List.of(new Card(SKIP), new Card(ATTACK), new Card(NOPE)), ann.cardChoices().getFirst().options());
        assertEquals(List.of(new Card(NOPE), new Card(SKIP), new Card(ATTACK), new Card(TACO_CAT)),
                game.state().deck().peek(4));
    }

    @Test
    void alterTheFuture_shortDeck_ordersWhatIsThere() {
        ScriptedController ann = ScriptedController.passing().chooses(List.of(1, 0));
        GameStateBuilder game = new GameStateBuilder()
                .player("Ann", ann).player("Bob", ScriptedController.passing())
                .deck(SKIP, ATTACK);

        new FakeAlterTheFuture().apply(game.buildContext(), game.player(0));

        assertEquals(List.of(new Card(ATTACK), new Card(SKIP)), game.state().deck().peek(3));
    }

    @Test
    void alterTheFuture_emptyDeck_asksNothing() {
        ScriptedController ann = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder().player("Ann", ann).player("Bob", ScriptedController.passing());

        new FakeAlterTheFuture().apply(game.buildContext(), game.player(0));

        assertTrue(ann.cardChoices().isEmpty());
    }
}