package ek.engine;

import ek.model.Card;
import ek.model.CardType;
import ek.model.Hand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static ek.fakeimploding.FakeCardType.FAKE_FERAL_CAT;
import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class PlayResolverTest {

    private final CardEffect attack = (ctx, actor) -> { };
    private final CardEffect pair = (ctx, actor) -> { };
    private final CardEffect triple = (ctx, actor) -> { };
    private final PlayResolver resolver = new PlayResolver(Map.of(ATTACK, attack), pair, triple);

    private static Hand hand(CardType... types) {
        Hand hand = new Hand();
        for (CardType t : types) {
            hand.add(new Card(t));
        }
        return hand;
    }

    @Test
    void singleCardWithEffect_resolvesToItsEffect() throws InvalidMoveException {
        assertSame(attack, resolver.resolve(List.of(ATTACK), hand(ATTACK)));
    }

    @Test
    void pairAndTriple_resolveToCombos_evenForCardsWithOwnEffect() throws InvalidMoveException {
        assertSame(pair, resolver.resolve(List.of(TACO_CAT, TACO_CAT), hand(TACO_CAT, TACO_CAT)));
        assertSame(pair, resolver.resolve(List.of(ATTACK, ATTACK), hand(ATTACK, ATTACK)));
        assertSame(triple, resolver.resolve(List.of(NOPE, NOPE, NOPE), hand(NOPE, NOPE, NOPE)));
    }

    @Test
    void cardWithoutEffect_aloneIsRejected() {
        assertThrows(InvalidMoveException.class, () -> resolver.resolve(List.of(TACO_CAT), hand(TACO_CAT)));
        assertThrows(InvalidMoveException.class, () -> resolver.resolve(List.of(DEFUSE), hand(DEFUSE)));
    }

    @Test
    void wildcard_matchesCatCategoryInPair() throws InvalidMoveException {
        assertSame(pair, resolver.resolve(List.of(FAKE_FERAL_CAT, TACO_CAT), hand(FAKE_FERAL_CAT, TACO_CAT)));
        assertSame(pair, resolver.resolve(List.of(CATTERMELON, FAKE_FERAL_CAT), hand(CATTERMELON, FAKE_FERAL_CAT)));
        assertSame(pair, resolver.resolve(List.of(FAKE_FERAL_CAT, FAKE_FERAL_CAT),
                hand(FAKE_FERAL_CAT, FAKE_FERAL_CAT)));
    }

    @Test
    void wildcard_inThreeOfAKind() throws InvalidMoveException {
        assertSame(triple, resolver.resolve(List.of(FAKE_FERAL_CAT, TACO_CAT, TACO_CAT),
                hand(TACO_CAT, FAKE_FERAL_CAT, TACO_CAT)));
        assertSame(triple, resolver.resolve(List.of(FAKE_FERAL_CAT, FAKE_FERAL_CAT, TACO_CAT),
                hand(FAKE_FERAL_CAT, TACO_CAT, FAKE_FERAL_CAT)));
    }

    @Test
    void wildcard_withNonCat_rejected() {
        assertThrows(InvalidMoveException.class,
                () -> resolver.resolve(List.of(FAKE_FERAL_CAT, ATTACK), hand(FAKE_FERAL_CAT, ATTACK)));
        assertThrows(InvalidMoveException.class,
                () -> resolver.resolve(List.of(FAKE_FERAL_CAT, TACO_CAT, CATTERMELON),
                        hand(FAKE_FERAL_CAT, TACO_CAT, CATTERMELON)));
    }

    @Test
    void wildcardAlone_rejected() {
        assertThrows(InvalidMoveException.class, () -> resolver.resolve(List.of(FAKE_FERAL_CAT), hand(FAKE_FERAL_CAT)));
    }

    @Test
    void wildcardPair_needsBothCardsInHand() {
        assertThrows(InvalidMoveException.class,
                () -> resolver.resolve(List.of(FAKE_FERAL_CAT, TACO_CAT), hand(FAKE_FERAL_CAT, CATTERMELON)));
        assertThrows(InvalidMoveException.class,
                () -> resolver.resolve(List.of(FAKE_FERAL_CAT, FAKE_FERAL_CAT), hand(FAKE_FERAL_CAT, TACO_CAT)));
    }

    @Test
    void cardsNotInHand_rejected() {
        assertThrows(InvalidMoveException.class, () -> resolver.resolve(List.of(ATTACK), hand()));
        assertThrows(InvalidMoveException.class,
                () -> resolver.resolve(List.of(TACO_CAT, TACO_CAT), hand(TACO_CAT)));
    }

    @Test
    void mixedTypes_fourOfAKind_orNothing_rejected() {
        assertThrows(InvalidMoveException.class,
                () -> resolver.resolve(List.of(TACO_CAT, CATTERMELON), hand(TACO_CAT, CATTERMELON)));
        assertThrows(InvalidMoveException.class,
                () -> resolver.resolve(List.of(SKIP, SKIP, SKIP, SKIP), hand(SKIP, SKIP, SKIP, SKIP)));
        assertThrows(InvalidMoveException.class, () -> resolver.resolve(List.of(), hand(SKIP)));
    }
}
