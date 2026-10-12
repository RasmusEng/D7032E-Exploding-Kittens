package ek.player;

import ek.model.CardType;

import java.io.Serializable;
import java.util.List;

/**
 * What a player chooses to do on their turn. Sealed, so the engine's {@code switch}
 * over actions is checked for completeness by the compiler.
 */
public sealed interface TurnAction extends Serializable {

    /** End the turn by drawing a card (rule 9a). */
    record Pass() implements TurnAction {
    }

    /** Play one card, or 2 or 3 cards of the same type as a combo (rule 9b). */
    record Play(List<CardType> cards) implements TurnAction {
        public Play {
            cards = List.copyOf(cards);
        }

        public Play(CardType... cards) {
            this(List.of(cards));
        }
    }
}
