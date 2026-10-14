package ek.app;

import ek.engine.GameEngine;
import ek.engine.ScriptedController;
import ek.expansion.Expansion;
import ek.expansion.FasterGame;
import ek.expansion.OneKittenLessExpansion;
import ek.expansion.TestOnlyExpansion;
import ek.model.Deck;
import ek.model.Player;
import ek.player.PlayerController;
import ek.rules.GameConfig;
import ek.util.Randomizer;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static ek.expansion.TestCardType.OTHER_TEST_CARD;
import static ek.expansion.TestCardType.TEST_CARD;
import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class GameAssemblerTest {

    /** Fisher–Yates with nextInt(bound) = bound - 1 swaps nothing, so the deck keeps its order. */
    private static final Randomizer NO_SHUFFLE = bound -> bound - 1;

    private static List<Player> players(int n) {
        return IntStream.range(0, n).mapToObj(i -> new Player("P" + i)).toList();
    }

    private static Deck deck(GameConfig config) {
        return new GameAssembler(config, NO_SHUFFLE).setUp(players(config.players()));
    }

    @Test
    void baseGame_hasNoExpansionCards() {
        Deck deck = deck(GameConfig.baseGame(2, 0));
        assertEquals(0, deck.count(TEST_CARD));
        assertEquals(0, deck.count(OTHER_TEST_CARD));
        assertEquals(1, deck.count(EXPLODING_KITTEN));
    }

    @Test
    void selectedExpansion_cardsInTheDeck_deselectedOneNot() {
        Deck deck = deck(GameConfig.of(2, 0, List.of(new TestOnlyExpansion()), List.of()));
        // with no shuffling the 3 Test Cards are at the bottom, below the dealt cards
        assertEquals(3, deck.count(TEST_CARD));
        assertEquals(0, deck.count(OTHER_TEST_CARD));
    }

    @Test
    void laterExpansionAdjustsTheBaseGame_whateverTheSelectionOrder() {
        Expansion oneKittenLess = new OneKittenLessExpansion();
        Expansion testOnly = new TestOnlyExpansion();
        for (List<Expansion> selected : List.of(List.of(oneKittenLess, testOnly), List.of(testOnly, oneKittenLess))) {
            Deck deck = deck(GameConfig.of(4, 0, selected, List.of()));
            assertEquals(4 - 2, deck.count(EXPLODING_KITTEN), selected.toString());
            assertEquals(1, deck.count(OTHER_TEST_CARD));
        }
    }

    @Test
    void nameableCards_includeTheSelectedExpansionsCards() {
        var assembler = new GameAssembler(GameConfig.of(2, 0, List.of(new TestOnlyExpansion()), List.of()), NO_SHUFFLE);
        assertTrue(assembler.nameableCards().contains(TEST_CARD));
        assertTrue(assembler.nameableCards().contains(DEFUSE));
        assertTrue(assembler.nameableCards().contains(EXPLODING_KITTEN));
    }

    @Test
    void playableCards_areTheCardsWithAnEffect() {
        var playable = new GameAssembler(GameConfig.baseGame(2, 0), NO_SHUFFLE).playableCards();
        assertTrue(playable.contains(ATTACK));
        assertFalse(playable.contains(TACO_CAT));
    }

    @Test
    void selectedVariant_isApplied() {
        Deck plain = deck(GameConfig.baseGame(2, 0));
        Deck faster = deck(GameConfig.of(2, 0, List.of(), List.of(new FasterGame())));
        // 34 cards left after dealing, a third (11) removed, then the kitten
        assertEquals(35, plain.size());
        assertEquals(24, faster.size());
    }

    @Test
    void assemble_buildsAnEngineForTheControllers() {
        Map<Player, PlayerController> controllers = new LinkedHashMap<>();
        players(2).forEach(p -> controllers.put(p, ScriptedController.passing()));
        GameEngine engine = new GameAssembler(GameConfig.baseGame(2, 0), NO_SHUFFLE)
                .assemble(controllers, List.of(), Runnable::run);
        assertNotNull(engine);
        controllers.keySet().forEach(p -> assertEquals(8, p.hand().size()));
    }
}
