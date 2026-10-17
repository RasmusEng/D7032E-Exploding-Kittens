package ek.expansion;

import ek.model.BaseCardType;
import ek.model.CardType;
import ek.model.Deck;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class DeckFactoryTest {

    @Test
    void build_baseGame_twoPlayers_has48Cards() {
        Deck deck = new DeckFactory(List.of(new BaseGame())).build(2);
        assertEquals(48, deck.size());
        assertEquals(2, deck.count(DEFUSE));
    }

    @Test
    void build_combinesAllExpansions() {
        Expansion extra = players -> Map.of(NOPE, 3);
        Deck deck = new DeckFactory(List.of(new BaseGame(), extra)).build(2);
        assertEquals(51, deck.size());
        assertEquals(8, deck.count(NOPE));
    }

    @Test
    void build_passesPlayerCountToExpansions() {
        Expansion perPlayer = players -> Map.of(TACO_CAT, players);
        assertEquals(4, new DeckFactory(List.of(perPlayer)).build(4).size());
    }

    @Test
    void build_laterExpansionAdjustsEarlierDeckCounts() {
        Expansion noDefuse = new Expansion() {
            public Map<CardType, Integer> deckCards(int players) {
                return Map.of(TACO_CAT, 1);
            }

            public Map<CardType, Integer> deckCards(Map<CardType, Integer> soFar, int players) {
                Map<CardType, Integer> cards = new LinkedHashMap<>(Expansion.super.deckCards(soFar, players));
                cards.put(DEFUSE, 0);
                return cards;
            }
        };
        Deck deck = new DeckFactory(List.of(new BaseGame(), noDefuse)).build(2);
        assertEquals(0, deck.count(DEFUSE));
        assertEquals(5, deck.count(TACO_CAT));
        assertEquals(47, deck.size());
    }

    @Test
    void build_negativeCount_throws() {
        Expansion broken = new Expansion() {
            public Map<CardType, Integer> deckCards(int players) {
                return Map.of();
            }

            public Map<CardType, Integer> deckCards(Map<CardType, Integer> soFar, int players) {
                return Map.of(DEFUSE, -1);
            }
        };
        assertThrows(IllegalStateException.class, () -> new DeckFactory(List.of(broken)).build(2));
    }

    @Test
    void addAfterDeal_baseGame_addsOneFewerKittenThanPlayers() {
        Deck deck = new Deck();
        new DeckFactory(List.of(new BaseGame())).addAfterDeal(deck, 4);
        assertEquals(3, deck.size());
        assertEquals(3, deck.count(EXPLODING_KITTEN));
    }

    @Test
    void addAfterDeal_laterExpansionAdjustsEarlierCounts() {
        Expansion oneKittenLess = new Expansion() {
            public Map<CardType, Integer> deckCards(int players) {
                return Map.of();
            }

            public Map<CardType, Integer> cardsAfterDeal(Map<CardType, Integer> soFar, int players) {
                Map<CardType, Integer> cards = new LinkedHashMap<>(soFar);
                cards.merge(EXPLODING_KITTEN, -1, Integer::sum);
                cards.put(TACO_CAT, 1);
                return cards;
            }
        };
        Deck deck = new Deck();
        new DeckFactory(List.of(new BaseGame(), oneKittenLess)).addAfterDeal(deck, 3);
        assertEquals(1, deck.count(EXPLODING_KITTEN));
        assertEquals(1, deck.count(TACO_CAT));
    }

    @Test
    void addAfterDeal_negativeCount_throws() {
        Expansion broken = new Expansion() {
            public Map<CardType, Integer> deckCards(int players) {
                return Map.of();
            }

            public Map<CardType, Integer> cardsAfterDeal(Map<CardType, Integer> soFar, int players) {
                return Map.of(EXPLODING_KITTEN, -1);
            }
        };
        assertThrows(IllegalStateException.class, () -> new DeckFactory(List.of(broken)).addAfterDeal(new Deck(), 2));
    }

    @Test
    void cardTypes_includesCardsAddedAfterDealing() {
        Set<CardType> types = new DeckFactory(List.of(new BaseGame())).cardTypes(5);
        assertTrue(types.containsAll(List.of(DEFUSE, TACO_CAT, EXPLODING_KITTEN)));
        assertEquals(BaseCardType.values().length, types.size());
    }

    @Test
    void build_noExpansions_emptyDeck() {
        assertTrue(new DeckFactory(List.of()).build(3).isEmpty());
    }
}
