package ek.expansion;

import ek.cards.AttackEffect;
import ek.cards.FavorEffect;
import ek.cards.SeeTheFutureEffect;
import ek.cards.ShuffleEffect;
import ek.cards.SkipEffect;
import ek.engine.CardEffect;
import ek.model.CardType;

import java.util.LinkedHashMap;
import java.util.Map;

import static ek.model.BaseCardType.*;

/**
 * The base game: the deck before dealing (rule 3) and the card effects (rule 11a–e).
 * Exploding Kittens are added later (rule 6).
 */
public final class BaseGame implements Expansion {

    @Override
    public Map<CardType, Integer> deckCards(int playerCount) {
        Map<CardType, Integer> cards = new LinkedHashMap<>();
        cards.put(DEFUSE, playerCount == 5 ? 1 : 2);
        cards.put(ATTACK, 4);
        cards.put(FAVOR, 4);
        cards.put(NOPE, 5);
        cards.put(SHUFFLE, 4);
        cards.put(SKIP, 4);
        cards.put(SEE_THE_FUTURE, 5);
        cards.put(HAIRY_POTATO_CAT, 4);
        cards.put(CATTERMELON, 4);
        cards.put(RAINBOW_RALPHING_CAT, 4);
        cards.put(TACO_CAT, 4);
        cards.put(OVERWEIGHT_BIKINI_CAT, 4);
        return cards;
    }

    @Override
    public Map<CardType, CardEffect> cardEffects() {
        return Map.of(
                ATTACK, new AttackEffect(),
                FAVOR, new FavorEffect(),
                SHUFFLE, new ShuffleEffect(),
                SKIP, new SkipEffect(),
                SEE_THE_FUTURE, new SeeTheFutureEffect());
    }
}
