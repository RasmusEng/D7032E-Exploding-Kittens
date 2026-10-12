package ek.cards;

import ek.engine.GameStateBuilder;
import ek.engine.ScriptedController;
import ek.model.Card;
import org.junit.jupiter.api.Test;

import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class ShuffleEffectTest {

    @Test
    void rule11c_shuffle_shufflesDeckWithInjectedRandomizer() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", ScriptedController.passing())
                .deck(ATTACK, SKIP, FAVOR)
                .randomizer(bound -> 0);
        new ShuffleEffect().apply(game.buildContext(), game.player(0));
        // same order as DeckTest.shuffle_usesInjectedRandomizer
        assertEquals(List.of(new Card(SKIP), new Card(FAVOR), new Card(ATTACK)),
                game.state().deck().peek(3));
    }
}
