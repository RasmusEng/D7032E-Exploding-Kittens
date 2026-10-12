package ek.combos;

import ek.engine.GameStateBuilder;
import ek.engine.ScriptedController;
import ek.model.Card;
import ek.player.HandChange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class TwoOfAKindTest {

    @Test
    void rule11f_twoOfAKind_stealsUniformlyRandomCard() {
        List<Integer> bounds = new ArrayList<>();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().targets(0))
                .player("B", ScriptedController.passing(), NOPE, SKIP, ATTACK)
                .randomizer(bound -> { bounds.add(bound); return 2; });
        new TwoOfAKind().apply(game.buildContext(), game.player(0));

        assertEquals(List.of(3), bounds);                   // every card in B's hand can be picked
        assertTrue(game.player(0).hand().contains(ATTACK));
        assertEquals(2, game.player(1).hand().size());
    }

    @Test
    void twoOfAKind_bothPlayersAreToldWhichCardWasStolen() {
        ScriptedController a = ScriptedController.passing().targets(0);
        ScriptedController b = ScriptedController.passing();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", a)
                .player("B", b, DEFUSE);
        new TwoOfAKind().apply(game.buildContext(), game.player(0));

        assertEquals(List.of(new HandChange.Received(new Card(DEFUSE), "B")), a.handChanges());
        assertEquals(List.of(new HandChange.Lost(new Card(DEFUSE), "A")), b.handChanges());
    }

    @Test
    void twoOfAKind_targetWithEmptyHand_getsNothing() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().targets(0))
                .player("B", ScriptedController.passing())
                .randomizer(bound -> fail("no card to pick"));
        new TwoOfAKind().apply(game.buildContext(), game.player(0));
        assertTrue(game.player(0).hand().isEmpty());
    }
}
