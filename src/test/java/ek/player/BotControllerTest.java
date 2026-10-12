package ek.player;

import ek.engine.GameEngine;
import ek.engine.GameEvent;
import ek.engine.GameState;
import ek.engine.GameStateBuilder;
import ek.engine.InvalidMoveException;
import ek.engine.PlayResolver;
import ek.engine.RecordingListener;
import ek.expansion.BaseGame;
import ek.expansion.DeckFactory;
import ek.model.BaseCardType;
import ek.model.Card;
import ek.model.CardType;
import ek.model.DiscardPile;
import ek.model.Hand;
import ek.model.Player;
import ek.player.GameView.OpponentView;
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

class BotControllerTest {

    private static final List<CardType> NAMEABLE = List.of(BaseCardType.values());

    private static BotController bot(long seed) {
        return new BotController(new JavaRandomizer(new Random(seed)),
                new BaseGame().cardEffects().keySet(), NAMEABLE);
    }

    private static GameView view(CardType... hand) {
        List<Card> cards = java.util.Arrays.stream(hand).map(Card::new).toList();
        return new GameView(cards, List.of(new OpponentView("A", 2, true)), 5, 1);
    }

    @Test
    void chooseAction_alwaysLegal() throws InvalidMoveException {
        PlayResolver resolver = GameStateBuilder.basePlayResolver();
        CardType[] hand = {ATTACK, TACO_CAT, TACO_CAT, TACO_CAT, NOPE, DEFUSE, CATTERMELON};
        Hand realHand = new Hand();
        for (CardType t : hand) {
            realHand.add(new Card(t));
        }
        for (long seed = 0; seed < 200; seed++) {
            TurnAction action = bot(seed).chooseAction(view(hand));
            if (action instanceof TurnAction.Play play) {
                assertNotNull(resolver.resolve(play.cards(), realHand), play.toString());
            }
        }
    }

    @Test
    void chooseAction_neverPlaysCardsWithoutEffectAlone() {
        for (long seed = 0; seed < 100; seed++) {
            TurnAction action = bot(seed).chooseAction(view(NOPE, DEFUSE, TACO_CAT));
            assertEquals(new TurnAction.Pass(), action);
        }
    }

    @Test
    void answers_alwaysInRange() {
        for (long seed = 0; seed < 100; seed++) {
            BotController bot = bot(seed);
            int position = bot.chooseKittenPosition(view(), 4);
            assertTrue(position >= 0 && position <= 4);
            int target = bot.chooseTarget(view(), List.of(new OpponentView("A", 1, true), new OpponentView("B", 1, true)));
            assertTrue(target >= 0 && target < 2);
            int gift = bot.chooseCardToGive(view(SKIP, NOPE, ATTACK), "A");
            assertTrue(gift >= 0 && gift < 3);
            assertTrue(NAMEABLE.contains(bot.nameCard(view())));
        }
    }

    @Test
    void rule16_gameWithOnlyBots_endsWithOneWinner() {
        for (int n = 2; n <= 5; n++) {
            JavaRandomizer randomizer = new JavaRandomizer(new Random(7 + n));
            List<Player> players = IntStream.rangeClosed(1, n).mapToObj(i -> new Player("Bot " + i)).toList();
            var deck = new GameSetup(new DeckFactory(List.of(new BaseGame())), randomizer).setUp(players);
            Map<Player, PlayerController> controllers = new LinkedHashMap<>();
            players.forEach(p -> controllers.put(p, new BotController(randomizer,
                    new BaseGame().cardEffects().keySet(), NAMEABLE)));
            RecordingListener listener = new RecordingListener();

            Player winner = new GameEngine(new GameState(players, deck, new DiscardPile()), controllers,
                    GameStateBuilder.basePlayResolver(), GameStateBuilder.instantNopeResolver(),
                    randomizer, List.of(listener)).run();

            assertEquals(List.of(winner), players.stream().filter(Player::isAlive).toList());
            assertTrue(listener.eventsOf(GameEvent.MoveRejected.class).isEmpty(), "bots only make legal plays");
        }
    }
}
