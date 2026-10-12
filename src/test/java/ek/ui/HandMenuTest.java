package ek.ui;

import ek.model.Card;
import ek.model.CardType;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class HandMenuTest {

    private static HandMenu menu(CardType... hand) {
        return new HandMenu(Arrays.stream(hand).map(Card::new).toList());
    }

    @Test
    void lines_groupEqualCards_sortedByName() {
        assertEquals(List.of("  1) Defuse", "  2) Skip", "  3) Taco Cat ×2"),
                menu(TACO_CAT, SKIP, TACO_CAT, DEFUSE).lines());
    }

    @Test
    void parsePlay_repeatedNumber_playsSeveralOfThatCard() {
        HandMenu menu = menu(TACO_CAT, SKIP, TACO_CAT);
        assertEquals(Optional.of(List.of(TACO_CAT, TACO_CAT)), menu.parsePlay("2 2"));
        assertEquals(Optional.of(List.of(SKIP)), menu.parsePlay(" 1 "));
    }

    @Test
    void parsePlay_moreThanInHand_orOutOfRange_isInvalid() {
        HandMenu menu = menu(TACO_CAT, SKIP);
        assertEquals(Optional.empty(), menu.parsePlay("1 1"));
        assertEquals(Optional.empty(), menu.parsePlay("3"));
        assertEquals(Optional.empty(), menu.parsePlay("0"));
        assertEquals(Optional.empty(), menu.parsePlay("x"));
        assertEquals(Optional.empty(), menu.parsePlay(""));
    }

    @Test
    void handIndexOf_mapsMenuNumberBackToTheHand() {
        HandMenu menu = menu(TACO_CAT, SKIP, DEFUSE);    // menu: Defuse, Skip, Taco Cat
        assertEquals(2, menu.handIndexOf(1));
        assertEquals(0, menu.handIndexOf(3));
    }

    @Test
    void descriptions_explainEachCardOnce() {
        List<String> help = menu(SKIP, SKIP).descriptions();
        assertEquals(List.of("  Skip: " + SKIP.description()), help);
    }
}
