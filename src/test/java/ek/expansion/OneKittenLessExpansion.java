package ek.expansion;

import ek.model.CardType;

import java.util.LinkedHashMap;
import java.util.Map;

import static ek.model.BaseCardType.EXPLODING_KITTEN;

/**
 * A fake expansion that, like Imploding Kittens, takes one Exploding Kitten out after the
 * deal and adds a card of its own instead. Its result depends on running after
 * {@link BaseGame}, so it checks that the expansions are chained in the right order.
 */
public final class OneKittenLessExpansion implements Expansion {

    @Override
    public String name() {
        return "One kitten less";
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    public Map<CardType, Integer> deckCards(int playerCount) {
        return Map.of();
    }

    @Override
    public Map<CardType, Integer> cardsAfterDeal(Map<CardType, Integer> soFar, int playerCount) {
        Map<CardType, Integer> cards = new LinkedHashMap<>(soFar);
        cards.merge(EXPLODING_KITTEN, -1, Integer::sum);
        cards.put(TestCardType.OTHER_TEST_CARD, 1);
        return cards;
    }
}
