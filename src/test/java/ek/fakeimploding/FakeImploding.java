package ek.fakeimploding;

import ek.engine.CardEffect;
import ek.engine.DrawEffect;
import ek.expansion.Expansion;
import ek.model.CardType;

import java.util.LinkedHashMap;
import java.util.Map;

import static ek.fakeimploding.FakeCardType.FAKE_ALTER_THE_FUTURE;
import static ek.fakeimploding.FakeCardType.FAKE_DRAW_FROM_THE_BOTTOM;
import static ek.fakeimploding.FakeCardType.FAKE_KITTEN;
import static ek.fakeimploding.FakeCardType.FAKE_REVERSE;
import static ek.fakeimploding.FakeCardType.FAKE_TARGETED_ATTACK;
import static ek.fakeimploding.FakeCardType.FAKE_UNSTOPPABLE;
import static ek.model.BaseCardType.DEFUSE;
import static ek.model.BaseCardType.EXPLODING_KITTEN;

/**
 * A test-only stand-in for Imploding Kittens, in its own package and registered with one line
 * in {@code src/test/resources/META-INF/services}. It uses only the extension points, so it
 * proves Imploding Kittens can be added without changing production code.
 */
public final class FakeImploding implements Expansion {

    private static final int MAX_PLAYERS = 6;

    @Override
    public String name() {
        return "Fake Imploding";
    }

    @Override
    public String description() {
        return "Test stand-in for Imploding Kittens.";
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public int maxPlayers() {
        return MAX_PLAYERS;
    }

    @Override
    public Map<CardType, Integer> deckCards(int playerCount) {
        return Map.of(FAKE_ALTER_THE_FUTURE, 2, FAKE_REVERSE, 2, FAKE_TARGETED_ATTACK, 2,
                FAKE_DRAW_FROM_THE_BOTTOM, 2, FAKE_UNSTOPPABLE, 2);
    }

    @Override
    public Map<CardType, CardEffect> cardEffects() {
        return Map.of(
                FAKE_ALTER_THE_FUTURE, new FakeAlterTheFuture(),
                FAKE_REVERSE, new FakeReverse(),
                FAKE_TARGETED_ATTACK, new FakeTargetedAttack(),
                FAKE_DRAW_FROM_THE_BOTTOM, new FakeDrawFromTheBottom(),
                FAKE_UNSTOPPABLE, (ctx, actor) -> ctx.state().turns().endTurn(ctx.state().players()));
    }

    /** With 6 players every Defuse is dealt, so none go into the deck. */
    @Override
    public Map<CardType, Integer> deckCards(Map<CardType, Integer> soFar, int playerCount) {
        Map<CardType, Integer> cards = new LinkedHashMap<>(Expansion.super.deckCards(soFar, playerCount));
        if (playerCount == MAX_PLAYERS) {
            cards.put(DEFUSE, 0);
        }
        return cards;
    }

    @Override
    public Map<CardType, DrawEffect> drawEffects() {
        return Map.of(FAKE_KITTEN, new FakeImplodingKittenDraw());
    }

    /** Like the Imploding Kitten, its kitten replaces one Exploding Kitten. */
    @Override
    public Map<CardType, Integer> cardsAfterDeal(Map<CardType, Integer> soFar, int playerCount) {
        Map<CardType, Integer> cards = new LinkedHashMap<>(soFar);
        cards.merge(EXPLODING_KITTEN, -1, Integer::sum);
        cards.put(FAKE_KITTEN, 1);
        return cards;
    }
}