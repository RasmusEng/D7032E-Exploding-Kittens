package ek.engine;

import ek.expansion.BaseGame;
import ek.expansion.DeckFactory;
import ek.model.Card;
import ek.model.DiscardPile;
import ek.model.Player;
import ek.player.HandChange;
import ek.player.PlayerController;
import ek.rules.GameSetup;
import ek.util.JavaRandomizer;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class GameEngineTest {

    @Test
    void fullGame_fromRealSetup_endsWithOneWinner() {
        for (int n = 2; n <= 5; n++) {
            JavaRandomizer randomizer = new JavaRandomizer(new Random(42 + n));
            List<Player> players = IntStream.range(0, n).mapToObj(i -> new Player("P" + i)).toList();
            var deck = new GameSetup(new DeckFactory(List.of(new BaseGame())), randomizer).setUp(players);
            Map<Player, PlayerController> controllers = new LinkedHashMap<>();
            players.forEach(p -> controllers.put(p, ScriptedController.passing().withDefaultPosition(0)));
            RecordingListener listener = new RecordingListener();

            Player winner = new GameEngine(new GameState(players, deck, new DiscardPile()),
                    controllers, GameStateBuilder.basePlayResolver(), GameStateBuilder.instantNopeResolver(),
                    randomizer, List.of(listener)).run();

            assertEquals(List.of(winner), players.stream().filter(Player::isAlive).toList());
            assertEquals(n - 1, listener.eventsOf(GameEvent.PlayerExploded.class).size());
            assertEquals(new GameEvent.Winner(winner), listener.events().get(listener.events().size() - 1));
        }
    }

    @Test
    void events_publishedInOrder() {
        RecordingListener listener = new RecordingListener();
        GameStateBuilder game = new GameStateBuilder()
                .player("A", ScriptedController.passing())
                .player("B", ScriptedController.passing())
                .deck(SKIP, EXPLODING_KITTEN)
                .listener(listener);
        game.buildEngine().run();
        Player a = game.player(0), b = game.player(1);

        assertEquals(List.of(
                new GameEvent.TurnStarted(a, 1), new GameEvent.CardDrawn(a),
                new GameEvent.TurnStarted(b, 1), new GameEvent.CardDrawn(b),
                new GameEvent.PlayerExploded(b), new GameEvent.Winner(a)), listener.events());
    }

    @Test
    void drawnCard_isToldOnlyToTheDrawer() {
        ScriptedController a = ScriptedController.passing();
        ScriptedController b = ScriptedController.passing();
        new GameStateBuilder()
                .player("A", a)
                .player("B", b)
                .deck(SKIP, EXPLODING_KITTEN)
                .startPlayer(0)
                .buildEngine().run();

        assertEquals(List.of(new HandChange.Drawn(new Card(SKIP))), a.handChanges());
        assertEquals(List.of(), b.handChanges());           // B drew the kitten: not added to the hand
    }

    @Test
    void controllerView_showsOwnHandAndOpponentsOnly() {
        ScriptedController a = ScriptedController.passing();
        new GameStateBuilder()
                .player("A", a, TACO_CAT)
                .player("B", ScriptedController.passing(), NOPE, NOPE)
                .deck(EXPLODING_KITTEN)
                .buildEngine().run();

        ek.player.GameView view = a.actionViews().get(0);
        assertEquals(1, view.hand().size());
        assertEquals(List.of(new ek.player.GameView.OpponentView("B", 2, true)), view.opponents());
        assertEquals(1, view.deckSize());
    }
}
