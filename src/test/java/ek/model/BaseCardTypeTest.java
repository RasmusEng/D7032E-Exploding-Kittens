package ek.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BaseCardTypeTest {

    @Test
    void rule12_everyBaseCard_canBeNoped() {
        // Defuse and the Exploding Kitten are never played alone; a pair of them is a combo, which can be Noped
        for (BaseCardType type : BaseCardType.values()) {
            assertTrue(type.canBeNoped(), type.displayName());
        }
    }
}