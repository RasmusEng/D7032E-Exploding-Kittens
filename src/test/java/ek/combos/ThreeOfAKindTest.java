package ek.combos;

import ek.engine.GameStateBuilder;
import ek.engine.ScriptedController;
import ek.model.Card;
import ek.player.HandChange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class ThreeOfAKindTest {

    @Test
    void rule11g_threeOfAKind_namedCardTaken() {
        ScriptedController a = ScriptedController.passing().targets(0).names(DEFUSE);
        GameStateBuilder game = new GameStateBuilder()
                .player("A", a)
                .player("B", ScriptedController.passing(), NOPE, DEFUSE);
        new ThreeOfAKind().apply(game.buildContext(), game.player(0));

        assertTrue(game.player(0).hand().contains(DEFUSE));
        assertFalse(game.player(1).hand().contains(DEFUSE));
        assertEquals(List.of(new HandChange.Received(new Card(DEFUSE), "B")), a.handChanges());
    }

    @Test
    void rule11g_threeOfAKind_targetLacksNamedCard_getsNothing() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().targets(0).names(DEFUSE))
                .player("B", ScriptedController.passing(), NOPE);
        new ThreeOfAKind().apply(game.buildContext(), game.player(0));

        assertTrue(game.player(0).hand().isEmpty());
        assertEquals(1, game.player(1).hand().size());
    }
}
