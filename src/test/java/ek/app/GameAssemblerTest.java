package ek.app;

import ek.cards.ExplodingKittenDraw;
import ek.engine.CardEffect;
import ek.engine.DrawEffect;
import ek.engine.GameEngine;
import ek.engine.InvalidMoveException;
import ek.engine.ScriptedController;
import ek.expansion.Expansion;
import ek.expansion.OneKittenLessExpansion;
import ek.expansion.TestOnlyExpansion;
import ek.expansion.TestOnlyVariant;
import ek.fakeimploding.FakeCardType;
import ek.fakeimploding.FakeImploding;
import ek.model.Card;
import ek.model.CardType;
import ek.model.Deck;
import ek.model.Hand;
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

    /** An expansion that only replaces what Skip does when played and Taco Cat when drawn. */
    private static Expansion replacing(int order, CardEffect skip, DrawEffect taco) {
        return new Expansion() {
            public Map<CardType, Integer> deckCards(int players) {
                return Map.of();
            }

            public Map<CardType, CardEffect> cardEffects() {
                return Map.of(SKIP, skip);
            }

            public Map<CardType, DrawEffect> drawEffects() {
                return Map.of(TACO_CAT, taco);
            }

            public int order() {
                return order;
            }
        };
    }

    @Test
    void laterExpansionsEffects_overrideEarlierOnes_inOrderNotListOrder() throws InvalidMoveException {
        CardEffect earlySkip = (ctx, actor) -> { };
        CardEffect lateSkip = (ctx, actor) -> { };
        DrawEffect earlyTaco = (ctx, actor, card, faceUp) -> DrawEffect.Outcome.SURVIVED;
        DrawEffect lateTaco = (ctx, actor, card, faceUp) -> DrawEffect.Outcome.SURVIVED;
        Expansion early = replacing(10, earlySkip, earlyTaco);
        Expansion late = replacing(20, lateSkip, lateTaco);
        // listed late first: the order() decides, not the list
        GameAssembler assembler = new GameAssembler(GameConfig.of(2, 0, List.of(late, early), List.of()), NO_SHUFFLE);

        Hand hand = new Hand();
        hand.add(new Card(SKIP));
        assertSame(lateSkip, assembler.playResolver().resolve(List.of(SKIP), hand));
        assertSame(lateTaco, assembler.drawEffects().get(TACO_CAT));
        assertInstanceOf(ExplodingKittenDraw.class, assembler.drawEffects().get(EXPLODING_KITTEN), "base kept");
    }

    @Test
    void implodingSixPlayers_assembledSetUp() {
        GameConfig config = GameConfig.of(6, 0, List.of(new FakeImploding()), List.of());
        List<Player> players = players(6);
        Deck deck = new GameAssembler(config, NO_SHUFFLE).setUp(players);

        players.forEach(p -> assertEquals(8, p.hand().size()));
        players.forEach(p -> assertEquals(1, p.hand().count(DEFUSE)));
        assertEquals(0, deck.count(DEFUSE));
        assertEquals(4, deck.count(EXPLODING_KITTEN));
        assertEquals(1, deck.count(FakeCardType.FAKE_KITTEN));
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
        Deck withVariant = deck(GameConfig.of(2, 0, List.of(), List.of(new TestOnlyVariant())));
        // the test variant removes one card after dealing
        assertEquals(35, plain.size());
        assertEquals(34, withVariant.size());
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
