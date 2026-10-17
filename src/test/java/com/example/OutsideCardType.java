package com.example;

import ek.model.CardType;

/** A card type outside {@code ek.}: what a third-party expansion that ignores the package rule would send. */
public enum OutsideCardType implements CardType {
    OUTSIDER;

    @Override
    public String displayName() {
        return "Outsider";
    }

    @Override
    public String description() {
        return "Only used in tests.";
    }
}