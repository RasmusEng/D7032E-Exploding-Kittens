package ek.fakeimploding;

import ek.expansion.BaseGame;
import ek.expansion.DeckFactory;
import ek.expansion.Expansion;
import ek.expansion.PlayerRange;
import ek.model.Deck;
import ek.model.Player;
import ek.rules.GameConfig;
import ek.rules.GameSetup;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static ek.fakeimploding.FakeCardType.FAKE_KITTEN;
import static ek.model.BaseCardType.DEFUSE;
import static ek.model.BaseCardType.EXPLODING_KITTEN;
import static org.junit.jupiter.api.Assertions.*;

/** The fake expansion on its own, before it is registered: it only uses the extension points. */
class FakeImplodingTest {

    private final List<Expansion> withFake = List.of(new BaseGame(), new FakeImploding());

    @Test
    void implodingSixPlayers_rangeIsTwoToSix() {
        assertEquals(new PlayerRange(2, 6), PlayerRange.of(withFake));
        assertEquals(new PlayerRange(2, 6), GameConfig.playerRange(List.of(new FakeImploding())));
        assertDoesNotThrow(() -> GameConfig.of(6, 0, List.of(new FakeImploding()), List.of()));
    }

    @Test
    void implodingSixPlayers_noExtraDefuseInDeck() {
        assertEquals(0, new DeckFactory(withFake).build(6).count(DEFUSE));
    }

    @Test
    void implodingFewerPlayers_keepsTheBaseDefuseCount() {
        DeckFactory factory = new DeckFactory(withFake);
        assertEquals(2, factory.build(4).count(DEFUSE));
        assertEquals(1, factory.build(5).count(DEFUSE));
    }

    @Test
    void implodingKitten_replacesOneExplodingKitten() {
        Deck deck = new Deck();
        new DeckFactory(withFake).addAfterDeal(deck, 6);
        assertEquals(4, deck.count(EXPLODING_KITTEN));
        assertEquals(1, deck.count(FAKE_KITTEN));
    }

    @Test
    void implodingSixPlayers_setUp_everyoneHasOneDefuse() {
        List<Player> players = IntStream.range(0, 6).mapToObj(i -> new Player("P" + i)).toList();
        Deck deck = new GameSetup(new DeckFactory(withFake), bound -> 0).setUp(players);
        players.forEach(p -> assertEquals(1, p.hand().count(DEFUSE), p.name()));
        assertEquals(0, deck.count(DEFUSE));
        assertEquals(5, deck.count(EXPLODING_KITTEN) + deck.count(FAKE_KITTEN));
    }
}