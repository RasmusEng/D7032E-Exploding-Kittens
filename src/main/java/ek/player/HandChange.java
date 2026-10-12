package ek.player;

import ek.model.Card;

import java.io.Serializable;
import java.util.Objects;

/**
 * A private change to one player's hand, telling only that player which card moved.
 * The public {@link ek.engine.GameEvent}s ({@code CardDrawn}, {@code CardTaken}) leave the
 * card out on purpose, so this is how the player learns it.
 */
public sealed interface HandChange extends Serializable {

    /** The card this change is about. */
    Card card();

    /** You drew {@code card} from the deck. */
    record Drawn(Card card) implements HandChange {
        public Drawn {
            Objects.requireNonNull(card, "card");
        }
    }

    /** {@code from} gave you {@code card} (Favor) or you took it from them (combos). */
    record Received(Card card, String from) implements HandChange {
        public Received {
            Objects.requireNonNull(card, "card");
            Objects.requireNonNull(from, "from");
        }
    }

    /** {@code card} went from your hand to {@code to}. */
    record Lost(Card card, String to) implements HandChange {
        public Lost {
            Objects.requireNonNull(card, "card");
            Objects.requireNonNull(to, "to");
        }
    }
}
