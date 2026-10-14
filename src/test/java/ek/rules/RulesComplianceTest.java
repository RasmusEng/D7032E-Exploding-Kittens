package ek.rules;

import ek.engine.GameEvent;
import ek.engine.GameStateBuilder;
import ek.engine.RecordingListener;
import ek.engine.ScriptedController;
import ek.expansion.BaseGame;
import ek.expansion.DeckFactory;
import ek.expansion.TestOnlyVariant;
import ek.model.Deck;
import ek.model.Player;
import ek.util.Randomizer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

/** Rules 1–7 (game setup), 8–10 and 13 (turn engine). Card effects (rule 11) are tested in ek.cards / ek.combos. */
class RulesComplianceTest {

    /** Fisher–Yates with nextInt(bound) = bound - 1 swaps nothing, so the deck keeps its order. */
    private static final Randomizer NO_SHUFFLE = bound -> bound - 1;

    private static List<Player> players(int n) {
        return IntStream.range(0, n).mapToObj(i -> new Player("P" + i)).toList();
    }

    private static Deck setUp(List<Player> players, Randomizer randomizer) {
        return new GameSetup(new DeckFactory(List.of(new BaseGame())), randomizer).setUp(players);
    }

    @Test
    void rule01_oneOrSixPlayers_rejected() {
        assertThrows(IllegalArgumentException.class, () -> setUp(players(1), NO_SHUFFLE));
        assertThrows(IllegalArgumentException.class, () -> setUp(players(6), NO_SHUFFLE));
    }

    @Test
    void rule01_twoToFivePlayers_accepted() {
        for (int n = 2; n <= 5; n++) {
            int count = n;
            assertDoesNotThrow(() -> setUp(players(count), NO_SHUFFLE));
        }
    }

    @Test
    void rule02_eachPlayerGetsOneDefuse() {
        List<Player> players = players(3);
        Deck deck = setUp(players, NO_SHUFFLE);
        players.forEach(p -> assertTrue(p.hand().contains(DEFUSE), p.name()));
        // the players' Defuses come on top of the 2 in the deck (rule 3a)
        int inHands = players.stream().mapToInt(p -> p.hand().count(DEFUSE)).sum();
        assertEquals(3 + 2, inHands + deck.count(DEFUSE));
    }

    @Test
    void rule03a_fivePlayers_onlyOneExtraDefuseInGame() {
        List<Player> players = players(5);
        Deck deck = setUp(players, NO_SHUFFLE);
        int inHands = players.stream().mapToInt(p -> p.hand().count(DEFUSE)).sum();
        assertEquals(5 + 1, inHands + deck.count(DEFUSE));
    }

    @Test
    void rule04_deckShuffledBeforeDealing() {
        List<Integer> bounds = new ArrayList<>();
        setUp(players(2), bound -> { bounds.add(bound); return bound - 1; });
        // first shuffle covers the full 48-card pre-deal deck
        assertEquals(48, bounds.get(0));
    }

    @Test
    void rule05_eachPlayerDealtSevenCards() {
        List<Player> players = players(4);
        setUp(players, NO_SHUFFLE);
        players.forEach(p -> assertEquals(1 + 7, p.hand().size(), p.name()));
    }

    @Test
    void rule06_deckHasOneFewerKittenThanPlayers() {
        for (int n = 2; n <= 5; n++) {
            List<Player> players = players(n);
            Deck deck = setUp(players, NO_SHUFFLE);
            assertEquals(n - 1, deck.count(EXPLODING_KITTEN), n + " players");
            players.forEach(p -> assertFalse(p.hand().contains(EXPLODING_KITTEN)));
        }
    }

    @Test
    void rule06_variantRunsAfterDealingBeforeTheKittens_stillOneFewerKittenThanPlayers() {
        for (int n = 2; n <= 5; n++) {
            List<Player> players = players(n);
            Deck deck = new GameSetup(new DeckFactory(List.of(new BaseGame())), NO_SHUFFLE,
                    List.of(new TestOnlyVariant())).setUp(players);
            int afterDeal = (n == 5 ? 47 : 48) - 7 * n;
            // the variant removed one card, and the kittens were added after it
            assertEquals(afterDeal - 1 + n - 1, deck.size(), n + " players");
            assertEquals(n - 1, deck.count(EXPLODING_KITTEN), n + " players");
            players.forEach(p -> assertEquals(1 + 7, p.hand().size()));
        }
    }

    @Test
    void rule07_deckShuffledAgainAfterKittensInserted() {
        List<Integer> bounds = new ArrayList<>();
        Deck deck = setUp(players(2), bound -> { bounds.add(bound); return bound - 1; });
        // 48 - 14 dealt + 1 kitten = 35 cards; second shuffle starts after the first's 47 calls
        assertEquals(35, deck.size());
        assertEquals(35, bounds.get(47));
        assertEquals(47 + 34, bounds.size());
    }

    @Test
    void rule07_kittensNotLeftAtBottom() {
        // always swap with index 0: moves the bottom card (a kitten) to the top
        Deck deck = setUp(players(2), bound -> 0);
        assertNotEquals(EXPLODING_KITTEN, deck.peek(deck.size()).get(deck.size() - 1).type());
    }

    // ---- Rules 8–13: turn engine ----

    @Test
    void rule08_randomizerPicksStartPlayer() {
        List<Integer> bounds = new ArrayList<>();
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", ScriptedController.passing())
                .player("C", ScriptedController.passing())
                .deck(EXPLODING_KITTEN, EXPLODING_KITTEN)
                .randomizer(bound -> { bounds.add(bound); return 2; })
                .listener(listener);
        game.buildEngine().run();

        assertEquals(List.of(3), bounds);
        assertEquals(game.player(2).name(), listener.eventsOf(GameEvent.TurnStarted.class).get(0).player());
    }

    @Test
    void rule09a_pass_drawsOneCardAndEndsTurn() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", ScriptedController.passing())
                .deck(SKIP, EXPLODING_KITTEN)
                .listener(listener);
        game.buildEngine().run();

        assertEquals(1, game.player(0).hand().count(SKIP));
        assertEquals(game.player(1).name(), listener.eventsOf(GameEvent.TurnStarted.class).get(1).player());
    }

    @Test
    void rule10_kittenWithDefuse_defuseDiscardedAndKittenReinserted() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing(1), DEFUSE)
                .player("B", ScriptedController.passing())
                .deck(EXPLODING_KITTEN, SKIP)
                .listener(listener);
        // A defuses and puts the kitten under SKIP; B draws SKIP; A draws the kitten and explodes
        Player winner = game.buildEngine().run();

        assertEquals(game.player(1), winner);
        assertEquals(List.of(new GameEvent.KittenDefused(game.player(0))),
                listener.eventsOf(GameEvent.KittenDefused.class));
        assertEquals(1, game.state().discardPile().cards().stream()
                .filter(c -> c.type() == DEFUSE).count());
    }

    @Test
    void rule10a_kittenInsertedOnTop() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing(0), DEFUSE)
                .player("B", ScriptedController.passing())
                .deck(EXPLODING_KITTEN, SKIP);
        // kitten back on top: B draws it next
        assertEquals(game.player(0), game.buildEngine().run());
        assertTrue(game.state().deck().peek(1).stream().allMatch(c -> c.type() == SKIP));
    }

    @Test
    void rule10a_kittenInsertedAtBottom() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing(2), DEFUSE)
                .player("B", ScriptedController.passing())
                .deck(EXPLODING_KITTEN, SKIP, ATTACK);
        // deck becomes SKIP, ATTACK, kitten: B draws SKIP, A draws ATTACK, B draws the kitten
        assertEquals(game.player(0), game.buildEngine().run());
        assertTrue(game.player(1).hand().isEmpty());          // B exploded and discarded SKIP
        assertTrue(game.player(0).hand().contains(ATTACK));
    }

    @Test
    void rule10a_positionOutsideDeck_askedAgain() {
        ScriptedController a = ScriptedController.passing(5, -1, 0);
        GameStateBuilder game = new GameStateBuilder()
                .player("A", a, DEFUSE)
                .player("B", ScriptedController.passing())
                .deck(EXPLODING_KITTEN, SKIP);
        assertEquals(game.player(0), game.buildEngine().run());
        assertEquals(3, a.positionRequests());
    }

    @Test
    void rule10b_kittenWithoutDefuse_playerExplodesAndDiscardsHand() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing(), SKIP, ATTACK)
                .player("B", ScriptedController.passing())
                .deck(EXPLODING_KITTEN);
        game.buildEngine().run();

        Player a = game.player(0);
        assertFalse(a.isAlive());
        assertTrue(a.hand().isEmpty());
        assertEquals(List.of(SKIP, ATTACK, EXPLODING_KITTEN),
                game.state().discardPile().cards().stream().map(c -> c.type()).toList());
    }

    @Test
    void rule10b_explodedPlayerTakesNoMoreTurns() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", ScriptedController.passing())
                .player("C", ScriptedController.passing())
                .deck(EXPLODING_KITTEN, SKIP, SKIP, EXPLODING_KITTEN)
                .listener(listener);
        // A explodes; B, C draw SKIP; B explodes; C wins
        assertEquals(game.player(2), game.buildEngine().run());
        assertEquals(1, listener.eventsOf(GameEvent.TurnStarted.class).stream()
                .filter(e -> e.player().equals(game.player(0).name())).count());
    }

    @Test
    void rule13_lastPlayerAlive_announcedToAllListeners() {
        RecordingListener first = new RecordingListener();
        RecordingListener second = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", ScriptedController.passing())
                .deck(EXPLODING_KITTEN)
                .listener(first)
                .listener(second);
        game.buildEngine().run();

        GameEvent.Winner expected = new GameEvent.Winner(game.player(1));
        assertEquals(List.of(expected), first.eventsOf(GameEvent.Winner.class));
        assertEquals(List.of(expected), second.eventsOf(GameEvent.Winner.class));
    }

    @Test
    void rule09b_playSeveralCardsThenPass_inOneTurn() {
        RecordingListener listener = new RecordingListener();
        ScriptedController a = ScriptedController.passing().play(SEE_THE_FUTURE).play(SHUFFLE).play(TACO_CAT, TACO_CAT).targets(0);
        GameStateBuilder game = new GameStateBuilder()
                .player("A", a, SEE_THE_FUTURE, SHUFFLE, TACO_CAT, TACO_CAT)
                .player("B", ScriptedController.passing(), NOPE)
                .deck(SKIP, EXPLODING_KITTEN)
                .startPlayer(0)
                .randomizer(bound -> bound - 1)       // shuffle keeps order
                .listener(listener);
        assertEquals(game.player(0), game.buildEngine().run());

        assertEquals(3, listener.eventsOf(GameEvent.CardsPlayed.class).size());
        assertEquals(2, listener.eventsOf(GameEvent.TurnStarted.class).size());   // A once, B once
        assertEquals(List.of(NOPE, SKIP), game.player(0).hand().cards().stream().map(c -> c.type()).toList());
        // A's 4 played cards, then the kitten B exploded on (B's Nope was stolen)
        assertEquals(List.of(SEE_THE_FUTURE, SHUFFLE, TACO_CAT, TACO_CAT, EXPLODING_KITTEN),
                game.state().discardPile().cards().stream().map(c -> c.type()).toList());
    }

    @Test
    void rule09b_playNoCards_justPass() {
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing(), ATTACK)
                .player("B", ScriptedController.passing())
                .deck(SKIP, EXPLODING_KITTEN);
        assertEquals(game.player(0), game.buildEngine().run());
        assertTrue(game.player(0).hand().contains(ATTACK));
    }

    @Test
    void rule09b_invalidPlay_rejectedAndPlayerChoosesAgain() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().play(TACO_CAT).play(SKIP), TACO_CAT)
                .player("B", ScriptedController.passing())
                .deck(SKIP, EXPLODING_KITTEN)
                .listener(listener);
        game.buildEngine().run();

        assertEquals(2, listener.eventsOf(GameEvent.MoveRejected.class).size());   // lone cat, missing Skip
        assertTrue(listener.eventsOf(GameEvent.CardsPlayed.class).isEmpty());
        assertTrue(game.player(0).hand().contains(TACO_CAT));
    }

    @Test
    void rule11_combo_ignoresCardsOwnEffect() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing().play(ATTACK, ATTACK).targets(0), ATTACK, ATTACK)
                .player("B", ScriptedController.passing(), NOPE)
                .deck(SKIP, EXPLODING_KITTEN)
                .listener(listener);
        game.buildEngine().run();

        // no Attack: A still draws, and B gets a normal single turn
        assertTrue(game.player(0).hand().contains(NOPE));
        assertTrue(game.player(0).hand().contains(SKIP));
        assertEquals(new GameEvent.TurnStarted(game.player(1), 1),
                listener.eventsOf(GameEvent.TurnStarted.class).get(1));
    }
}
