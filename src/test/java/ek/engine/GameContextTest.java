package ek.engine;

import ek.model.Card;
import ek.player.CardChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class GameContextTest {

    private static final CardChoice ORDER_OF_TWO =
            CardChoice.orderOf("Order", List.of(new Card(SKIP), new Card(NOPE)));

    @Test
    void chooseCards_validAnswer_returned() {
        ScriptedController ann = ScriptedController.passing().chooses(List.of(1, 0));
        GameStateBuilder game = new GameStateBuilder().player("Ann", ann).player("Bob", ScriptedController.passing());
        GameContext ctx = game.buildContext();

        assertEquals(List.of(1, 0), ctx.chooseCards(game.player(0), ORDER_OF_TWO));
        assertEquals(List.of(ORDER_OF_TWO), ann.cardChoices());
    }

    @Test
    void chooseCards_invalidAnswer_asksAgain() {
        ScriptedController ann = ScriptedController.passing().chooses(List.of(0, 0), List.of(0), List.of(0, 1));
        GameStateBuilder game = new GameStateBuilder().player("Ann", ann).player("Bob", ScriptedController.passing());

        assertEquals(List.of(0, 1), game.buildContext().chooseCards(game.player(0), ORDER_OF_TWO));
        assertEquals(3, ann.cardChoices().size());
    }

    @Test
    void askUntil_asksUntilTheAnswerIsValid() {
        AtomicInteger next = new AtomicInteger();
        assertEquals(3, GameContext.askUntil(next::incrementAndGet, n -> n >= 3));
    }
}