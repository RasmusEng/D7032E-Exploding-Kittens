package ek.expansion;

import ek.model.CardType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class BaseGameTest {

    private final BaseGame base = new BaseGame();

    @Test
    void rule03a_twoToFourPlayers_deckHasTwoDefuse() {
        for (int players = 2; players <= 4; players++) {
            assertEquals(2, base.deckCards(players).get(DEFUSE), players + " players");
        }
    }

    @Test
    void rule03a_fivePlayers_deckHasOneDefuse() {
        assertEquals(1, base.deckCards(5).get(DEFUSE));
    }

    @Test
    void rule03b_l_baseCardCounts() {
        Map<CardType, Integer> cards = base.deckCards(3);
        assertEquals(4, cards.get(ATTACK));                 // b
        assertEquals(4, cards.get(FAVOR));                  // c
        assertEquals(5, cards.get(NOPE));                   // d
        assertEquals(4, cards.get(SHUFFLE));                // e
        assertEquals(4, cards.get(SKIP));                   // f
        assertEquals(5, cards.get(SEE_THE_FUTURE));         // g
        assertEquals(4, cards.get(HAIRY_POTATO_CAT));       // h
        assertEquals(4, cards.get(CATTERMELON));            // i
        assertEquals(4, cards.get(RAINBOW_RALPHING_CAT));   // j
        assertEquals(4, cards.get(TACO_CAT));               // k
        assertEquals(4, cards.get(OVERWEIGHT_BIKINI_CAT));  // l
    }

    @Test
    void rule03_noExplodingKittensBeforeDealing() {
        assertFalse(base.deckCards(5).containsKey(EXPLODING_KITTEN));
    }
}
