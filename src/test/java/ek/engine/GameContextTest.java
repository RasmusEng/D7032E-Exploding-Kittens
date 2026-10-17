package ek.engine;

import ek.model.Card;
import ek.model.Deck;
import ek.player.CardChoice;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
    void drawAndEndTurn_top_cardGoesToHand_andOneTurnEnds() {
        GameStateBuilder game = new GameStateBuilder()
                .player("Ann", ScriptedController.passing()).player("Bob", ScriptedController.passing())
                .deck(TACO_CAT, SKIP);
        GameContext ctx = game.buildContext();

        ctx.drawAndEndTurn(game.player(0), Deck.Side.TOP);

        assertTrue(game.player(0).hand().contains(TACO_CAT));
        assertEquals(1, game.state().turns().currentIndex());
    }

    @Test
    void drawAndEndTurn_bottom_drawsTheBottomCard() {
        GameStateBuilder game = new GameStateBuilder()
                .player("Ann", ScriptedController.passing()).player("Bob", ScriptedController.passing())
                .deck(TACO_CAT, SKIP);

        game.buildContext().drawAndEndTurn(game.player(0), Deck.Side.BOTTOM);

        assertTrue(game.player(0).hand().contains(SKIP));
        assertEquals(List.of(new Card(TACO_CAT)), game.state().deck().peek(2));
    }

    @Test
    void drawAndEndTurn_drawEffectSaysOut_playMovesOnAndTurnsAreDropped() {
        DrawEffect out = (ctx, actor, card, faceUp) -> DrawEffect.Outcome.EXPLODED;
        GameStateBuilder game = new GameStateBuilder()
                .drawEffects(Map.of(TACO_CAT, out))
                .player("Ann", ScriptedController.passing()).player("Bob", ScriptedController.passing())
                .player("Cid", ScriptedController.passing())
                .deck(TACO_CAT);
        GameContext ctx = game.buildContext();
        game.state().turns().attack(game.state().players(), game.player(1));     // Bob: 2 turns

        ctx.drawAndEndTurn(game.player(1), Deck.Side.TOP);

        assertEquals(2, game.state().turns().currentIndex());
        assertEquals(1, game.state().turns().turnsLeft());
    }

    @Test
    void drawAndEndTurn_tellsTheDrawEffectWhetherTheCardWasFaceUp() {
        List<Boolean> seen = new java.util.ArrayList<>();
        DrawEffect record = (ctx, actor, card, faceUp) -> {
            seen.add(faceUp);
            return DrawEffect.Outcome.SURVIVED;
        };
        GameStateBuilder game = new GameStateBuilder()
                .drawEffects(Map.of(TACO_CAT, record))
                .player("Ann", ScriptedController.passing()).player("Bob", ScriptedController.passing())
                .deck(TACO_CAT, TACO_CAT);
        GameContext ctx = game.buildContext();
        game.state().deck().insertFaceUpAt(0, new Card(TACO_CAT));

        ctx.drawAndEndTurn(game.player(0), Deck.Side.TOP);
        ctx.drawAndEndTurn(game.player(1), Deck.Side.BOTTOM);

        assertEquals(List.of(true, false), seen);
    }

    @Test
    void viewFor_everyoneSeesTheFaceUpPositions() {
        GameStateBuilder game = new GameStateBuilder()
                .player("Ann", ScriptedController.passing()).player("Bob", ScriptedController.passing())
                .deck(SKIP, NOPE);
        GameContext ctx = game.buildContext();
        game.state().deck().insertFaceUpAt(1, new Card(EXPLODING_KITTEN));

        assertEquals(List.of(1), ctx.viewFor(game.player(0)).faceUpPositions());
        assertEquals(List.of(1), ctx.viewFor(game.player(1)).faceUpPositions());
    }

    @Test
    void reverseDirection_reversesPlay_andTellsEveryone() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("Ann", ScriptedController.passing()).player("Bob", ScriptedController.passing())
                .listener(listener);
        GameContext ctx = game.buildContext();

        ctx.reverseDirection(game.player(0));

        assertFalse(game.state().turns().isClockwise());
        assertEquals(List.of(new GameEvent.DirectionReversed("Ann")), listener.events());
    }

    @Test
    void putBackFaceUp_atTheChosenPosition_andTellsEveryoneWhichCard() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("Ann", ScriptedController.passing(2)).player("Bob", ScriptedController.passing())
                .deck(SKIP, NOPE)
                .listener(listener);
        GameContext ctx = game.buildContext();

        ctx.putBackFaceUp(game.player(0), new Card(EXPLODING_KITTEN));

        assertEquals(List.of(2), game.state().deck().faceUpPositions());
        assertEquals(List.of(new GameEvent.CardPutBackFaceUp("Ann", EXPLODING_KITTEN)), listener.events());
    }

    @Test
    void eliminate_discardsHandAndCause_andTellsEveryoneWhy() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("Ann", ScriptedController.passing(), DEFUSE, SKIP).player("Bob", ScriptedController.passing())
                .listener(listener);
        GameContext ctx = game.buildContext();

        ctx.eliminate(game.player(0), new Card(TACO_CAT));

        assertFalse(game.player(0).isAlive());
        assertEquals(3, game.state().discardPile().size());
        assertEquals(List.of(new GameEvent.PlayerEliminated("Ann", TACO_CAT)), listener.events());
    }

    @Test
    void askUntil_asksUntilTheAnswerIsValid() {
        AtomicInteger next = new AtomicInteger();
        assertEquals(3, GameContext.askUntil(next::incrementAndGet, n -> n >= 3));
    }
}