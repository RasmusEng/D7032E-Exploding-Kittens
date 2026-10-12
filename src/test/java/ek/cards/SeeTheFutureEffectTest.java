package ek.cards;

import ek.engine.GameStateBuilder;
import ek.engine.ScriptedController;
import ek.model.Card;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class SeeTheFutureEffectTest {

    @Test
    void rule11e_seeTheFuture_showsTopThreePrivatelyWithoutChangingDeck() {
        ScriptedController a = ScriptedController.passing();
        ScriptedController b = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", a)
                .player("B", b)
                .deck(ATTACK, SKIP, FAVOR, NOPE);
        new SeeTheFutureEffect().apply(game.buildContext(), game.player(0));

        assertEquals(List.of(List.of(new Card(ATTACK), new Card(SKIP), new Card(FAVOR))), a.futuresSeen());
        assertTrue(b.futuresSeen().isEmpty());
        assertEquals(4, game.state().deck().size());
    }

    @Test
    void rule11e_seeTheFuture_fewerThanThreeCards_showsWhatIsThere() {
        ScriptedController a = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", a)
                .player("B", ScriptedController.passing())
                .deck(EXPLODING_KITTEN);
        new SeeTheFutureEffect().apply(game.buildContext(), game.player(0));
        assertEquals(List.of(List.of(new Card(EXPLODING_KITTEN))), a.futuresSeen());
    }
}
