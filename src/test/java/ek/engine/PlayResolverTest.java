package ek.engine;

import ek.model.Card;
import ek.model.CardType;
import ek.model.Hand;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
