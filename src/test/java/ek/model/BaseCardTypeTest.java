package ek.model;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.Set;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class BaseCardTypeTest {

    @Test
    void theFiveCatCards_areCats() {
        Set<BaseCardType> cats = EnumSet.of(HAIRY_POTATO_CAT, CATTERMELON, RAINBOW_RALPHING_CAT, TACO_CAT,
                OVERWEIGHT_BIKINI_CAT);
        for (BaseCardType type : BaseCardType.values()) {
            assertEquals(cats.contains(type), type.isCat(), type.displayName());
        }
    }

    @Test
    void noBaseCard_isAWildcard() {
        for (BaseCardType type : BaseCardType.values()) {
            for (BaseCardType other : BaseCardType.values()) {
                assertFalse(type.substitutesFor(other), type + " for " + other);
            }
        }
    }

    @Test
    void rule12_everyBaseCard_canBeNoped() {
        // Defuse and the Exploding Kitten are never played alone; a pair of them is a combo, which can be Noped
        for (BaseCardType type : BaseCardType.values()) {
            assertTrue(type.canBeNoped(), type.displayName());
        }
    }
}