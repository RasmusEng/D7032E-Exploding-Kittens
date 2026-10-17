package ek.net;

import ek.engine.GameEvent;
import ek.model.Card;
import ek.model.CardType;
import ek.player.CardChoice;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.player.HandChange;
import ek.player.TurnAction;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

/**
 * The network protocol: one record per message, no keywords (rule 17). Most messages
 * mirror one {@link ek.player.PlayerController} call or its answer. Both directions are
 * sealed, so a {@code switch} over them is checked for completeness by the compiler.
 *
 * <p>Received records are rebuilt through their canonical constructors, so the checks
 * here also validate what comes off the wire.
 */
public sealed interface Message extends Serializable {

    /** Server to client. */
    sealed interface ToClient extends Message {
    }

    /** Client to server. */
    sealed interface ToServer extends Message {
    }

    /** First message to a client: its seat name and the cards it may name for three of a kind. */
    record Welcome(String name, List<CardType> nameableCards) implements ToClient {
        public Welcome {
            Objects.requireNonNull(name, "name");
            nameableCards = List.copyOf(nameableCards);
        }
    }

    record Event(GameEvent event) implements ToClient {
        public Event {
            Objects.requireNonNull(event, "event");
        }
    }

    record ChooseAction(GameView view) implements ToClient {
        public ChooseAction {
            Objects.requireNonNull(view, "view");
        }
    }

    record ChooseKittenPosition(GameView view, int deckSize) implements ToClient {
        public ChooseKittenPosition {
            Objects.requireNonNull(view, "view");
        }
    }

    record ChooseTarget(GameView view, List<OpponentView> candidates) implements ToClient {
        public ChooseTarget {
            Objects.requireNonNull(view, "view");
            candidates = List.copyOf(candidates);
        }
    }

    record ChooseCardToGive(GameView view, String requester) implements ToClient {
        public ChooseCardToGive {
            Objects.requireNonNull(view, "view");
            Objects.requireNonNull(requester, "requester");
        }
    }

    record NameCard(GameView view) implements ToClient {
        public NameCard {
            Objects.requireNonNull(view, "view");
        }
    }

    /** Any choice about cards (e.g. Alter the Future); answered with {@link CardsChosen}. */
    record ChooseCards(GameView view, CardChoice choice) implements ToClient {
        public ChooseCards {
            Objects.requireNonNull(view, "view");
            Objects.requireNonNull(choice, "choice");
        }
    }

    record SeeTheFuture(List<Card> topCards) implements ToClient {
        public SeeTheFuture {
            topCards = List.copyOf(topCards);
        }
    }

    /** The cards are dealt: this client's view of the table before the first turn. */
    record GameStarted(GameView view) implements ToClient {
        public GameStarted {
            Objects.requireNonNull(view, "view");
        }
    }

    /** Private: which card entered or left this client's hand. */
    record HandChanged(HandChange change) implements ToClient {
        public HandChanged {
            Objects.requireNonNull(change, "change");
        }
    }

    /**
     * A Nope window opened; {@code offerId} links the later messages to this offer.
     * {@code windowMillis} is its length, so the client can count down from when it arrives
     * (a plain number, so the {@link ObjectStreamChannel#FILTER} needs no {@code java.time} classes).
     * {@code canNope} says whether this player holds a Nope; every player but the last one to
     * play gets the offer either way.
     */
    record OfferNope(int offerId, GameView view, String playedBy, List<CardType> cards, int nopesSoFar,
                     long windowMillis, boolean canNope) implements ToClient {
        public OfferNope {
            Objects.requireNonNull(view, "view");
            Objects.requireNonNull(playedBy, "playedBy");
            if (windowMillis < 0) {
                throw new IllegalArgumentException("windowMillis < 0");
            }
            cards = List.copyOf(cards);
        }
    }

    /**
     * A Nope window opened on this client's own play (or Nope), shown as a countdown while the
     * others decide. It shares the offer ids, so {@link NopeClosed} ends it like an offer.
     */
    record WatchNopeWindow(int offerId, GameView view, String playedBy, List<CardType> cards, int nopesSoFar,
                           long windowMillis) implements ToClient {
        public WatchNopeWindow {
            Objects.requireNonNull(view, "view");
            Objects.requireNonNull(playedBy, "playedBy");
            if (windowMillis < 0) {
                throw new IllegalArgumentException("windowMillis < 0");
            }
            cards = List.copyOf(cards);
        }
    }

    /** The offer's (or watched window's) window has closed; the client stops its countdown. */
    record NopeClosed(int offerId) implements ToClient {
    }

    /** The answer to {@link PlayNope}: whether the Nope was in time. */
    record NopeResult(int offerId, boolean accepted) implements ToClient {
    }

    record ActionChosen(TurnAction action) implements ToServer {
        public ActionChosen {
            Objects.requireNonNull(action, "action");
        }
    }

    /** Answer to the requests that pick a number (kitten position, target, card to give). */
    record NumberChosen(int number) implements ToServer {
    }

    record CardNamed(CardType type) implements ToServer {
        public CardNamed {
            Objects.requireNonNull(type, "type");
        }
    }

    /** Answer to {@link ChooseCards}: indexes into its options, in the chosen order. */
    record CardsChosen(List<Integer> indexes) implements ToServer {
        public CardsChosen {
            indexes = List.copyOf(indexes);
        }
    }

    /** The client plays Nope on the given offer. */
    record PlayNope(int offerId) implements ToServer {
    }
}
