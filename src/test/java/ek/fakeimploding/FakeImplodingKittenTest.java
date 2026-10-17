package ek.fakeimploding;

import ek.engine.GameContext;
import ek.engine.GameStateBuilder;
import ek.engine.ScriptedController;
import ek.model.Card;
import ek.model.Deck;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.fakeimploding.FakeCardType.FAKE_KITTEN;
import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** The Imploding Kitten through face-up cards in the deck and {@code DrawEffect}'s face-up flag. */
class FakeImplodingKittenTest {

    @Test
    void faceUpKitten_drawnFaceDown_goesBackFaceUp_andEveryoneSeesWhere() {
        ScriptedController ann = ScriptedController.passing(1);       // 2nd from the top
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("Ann", ann, SKIP).player("Bob", ScriptedController.passing())
                .deck(FAKE_KITTEN, SKIP, EXPLODING_KITTEN);
        GameContext ctx = game.buildContext();

        ctx.drawAndEndTurn(game.player(0), Deck.Side.TOP);

        assertTrue(game.player(0).isAlive(), "no Defuse needed");
        assertEquals(List.of(new Card(SKIP), new Card(FAKE_KITTEN), new Card(EXPLODING_KITTEN)),
                game.state().deck().peek(3));
        assertEquals(List.of(1), game.state().deck().faceUpPositions());
        assertEquals(List.of(1), ctx.viewFor(game.player(1)).faceUpPositions());
        assertEquals(1, game.state().turns().currentIndex(), "the turn ended");
    }

    @Test
    void faceUpKitten_drawnFaceUp_playerOutWithoutDefuse() {
        ScriptedController ann = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .expansion(new FakeImploding())
                .player("Ann", ann, DEFUSE).player("Bob", ScriptedController.passing())
                .player("Cid", ScriptedController.passing())
                .deck(SKIP);
        GameContext ctx = game.buildContext();
        game.state().deck().insertFaceUpAt(0, new Card(FAKE_KITTEN));

        ctx.drawAndEndTurn(game.player(0), Deck.Side.TOP);

        assertFalse(game.player(0).isAlive());
        assertEquals(0, ann.positionRequests(), "a Defuse doesn't help");
        assertEquals(2, game.state().discardPile().size(), "the hand and the kitten");
        assertEquals(1, game.state().turns().currentIndex());
    }

    @Test
    void faceUpKitten_staysFaceUpThroughAlterTheFuture() {
        ScriptedController ann = ScriptedController.passing().chooses(List.of(2, 0, 1));
        GameStateBuilder game = new GameStateBuilder()
                .player("Ann", ann).player("Bob", ScriptedController.passing())
                .deck(SKIP, NOPE);
        GameContext ctx = game.buildContext();
        game.state().deck().insertFaceUpAt(0, new Card(FAKE_KITTEN));

        new FakeAlterTheFuture().apply(ctx, game.player(0));

        assertEquals(List.of(new Card(NOPE), new Card(FAKE_KITTEN), new Card(SKIP)), game.state().deck().peek(3));
        assertEquals(List.of(1), game.state().deck().faceUpPositions());
    }
}