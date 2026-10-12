package ek.engine;

import ek.model.CardType;
import ek.model.Hand;

import java.util.List;
import java.util.Map;

/**
 * Checks that a set of played cards is a legal play (rule 9b, 11) and returns its effect:
 * a single card with an effect, or 2 or 3 cards of the same type as a combo.
 */
public final class PlayResolver {

    private final Map<CardType, CardEffect> cardEffects;
    private final CardEffect twoOfAKind;
    private final CardEffect threeOfAKind;

    public PlayResolver(Map<CardType, CardEffect> cardEffects,
                        CardEffect twoOfAKind, CardEffect threeOfAKind) {
        this.cardEffects = Map.copyOf(cardEffects);
        this.twoOfAKind = twoOfAKind;
        this.threeOfAKind = threeOfAKind;
    }

    /**
     * @throws InvalidMoveException if the cards are not in {@code hand} or don't form a legal play
     */
    public CardEffect resolve(List<CardType> cards, Hand hand) throws InvalidMoveException {
        if (cards.isEmpty()) {
            throw new InvalidMoveException("No cards played");
        }
        CardType type = cards.get(0);
        if (!cards.stream().allMatch(type::equals)) {
            throw new InvalidMoveException("Combo cards must all have the same name");
        }
        if (hand.count(type) < cards.size()) {
            throw new InvalidMoveException("You don't have " + cards.size() + "× " + type.displayName());
        }
        return switch (cards.size()) {
            case 1 -> single(type);
            case 2 -> twoOfAKind;
            case 3 -> threeOfAKind;
            default -> throw new InvalidMoveException("Play 1 card, a pair or three of a kind");
        };
    }

    private CardEffect single(CardType type) throws InvalidMoveException {
        CardEffect effect = cardEffects.get(type);
        if (effect == null) {
            throw new InvalidMoveException(type.displayName() + " can't be played on its own");
        }
        return effect;
    }
}
