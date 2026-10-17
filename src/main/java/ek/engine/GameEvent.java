package ek.engine;

import ek.model.CardType;
import ek.model.Player;

import java.io.Serializable;
import java.util.List;

/**
 * Something that happened in the game, published to every {@link GameEventListener}.
 *
 * <p>Events are immutable values that name players instead of holding {@link Player}
 * objects, so a listener cannot reach anyone's hand, and an event can be sent over the
 * network as it is. Each event has a convenience constructor taking a {@code Player}.
 */
public sealed interface GameEvent extends Serializable {

    record TurnStarted(String player, int turnsLeft) implements GameEvent {
        public TurnStarted(Player player, int turnsLeft) {
            this(player.name(), turnsLeft);
        }
    }

    /** A player drew a card. Which card is not included: only that player may know it. */
    record CardDrawn(String player) implements GameEvent {
        public CardDrawn(Player player) {
            this(player.name());
        }
    }

    record CardsPlayed(String player, List<CardType> cards) implements GameEvent {
        public CardsPlayed {
            cards = List.copyOf(cards);
        }

        public CardsPlayed(Player player, List<CardType> cards) {
            this(player.name(), cards);
        }
    }

    /**
     * A Nope was played on the pending play.
     *
     * @param cancels whether the play is now cancelled (Nope) or back on (Yup)
     */
    record NopePlayed(String player, boolean cancels) implements GameEvent {
        public NopePlayed(Player player, boolean cancels) {
            this(player.name(), cancels);
        }
    }

    /** The play was Noped: its cards stay on the discard pile and nothing happens. */
    record PlayCancelled(String player, List<CardType> cards) implements GameEvent {
        public PlayCancelled {
            cards = List.copyOf(cards);
        }

        public PlayCancelled(Player player, List<CardType> cards) {
            this(player.name(), cards);
        }
    }

    /** A play was not allowed; nothing changed and the player chooses again. */
    record MoveRejected(String player, String reason) implements GameEvent {
        public MoveRejected(Player player, String reason) {
            this(player.name(), reason);
        }
    }

    /** A card moved from one hand to another (Favor, combos). Which card is private. */
    record CardTaken(String from, String to) implements GameEvent {
        public CardTaken(Player from, Player to) {
            this(from.name(), to.name());
        }
    }

    record KittenDefused(String player) implements GameEvent {
        public KittenDefused(Player player) {
            this(player.name());
        }
    }

    record PlayerExploded(String player) implements GameEvent {
        public PlayerExploded(Player player) {
            this(player.name());
        }
    }

    /** Play now goes the other way round the table (Reverse). */
    record DirectionReversed(String player) implements GameEvent {
        public DirectionReversed(Player player) {
            this(player.name());
        }
    }

    /**
     * A card went back into the deck face up (an Imploding Kitten). Which card it is, is
     * public: everyone can see it; where it lies is in every {@code GameView}.
     */
    record CardPutBackFaceUp(String player, CardType card) implements GameEvent {
        public CardPutBackFaceUp(Player player, CardType card) {
            this(player.name(), card);
        }
    }

    /** A player is out of the game because of a card other than an Exploding Kitten (an Imploding Kitten). */
    record PlayerEliminated(String player, CardType cause) implements GameEvent {
        public PlayerEliminated(Player player, CardType cause) {
            this(player.name(), cause);
        }
    }

    /** The last player alive (rule 13). */
    record Winner(String player) implements GameEvent {
        public Winner(Player player) {
            this(player.name());
        }
    }
}
