package ek.engine;

import ek.model.CardType;
import ek.model.Hand;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks that a set of played cards is a legal play (rule 9b, 11) and returns its effect:
 * a single card with an effect, or 2 or 3 cards of one kind as a combo. One kind means the
 * same type, where a wildcard (Feral Cat) may stand in for the type it matches.
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
        if (!formOneKind(cards)) {
            throw new InvalidMoveException("Combo cards must all have the same name (or be a wildcard for it)");
        }
        Map<CardType, Long> counts = cards.stream()
                .collect(Collectors.groupingBy(t -> t, LinkedHashMap::new, Collectors.counting()));
        for (Map.Entry<CardType, Long> entry : counts.entrySet()) {
            if (hand.count(entry.getKey()) < entry.getValue()) {
                throw new InvalidMoveException("You don't have " + entry.getValue() + "× "
                        + entry.getKey().displayName());
            }
        }
        return switch (cards.size()) {
            case 1 -> single(cards.getFirst());
            case 2 -> twoOfAKind;
            case 3 -> threeOfAKind;
            default -> throw new InvalidMoveException("Play 1 card, a pair or three of a kind");
        };
    }

    /**
     * Whether the cards are all of one kind: some played type such that every card is that
     * type or {@linkplain CardType#substitutesFor stands in for it} (a Feral Cat with a Taco
     * Cat). The card types decide, so a new wildcard needs no change here.
     */
    private static boolean formOneKind(List<CardType> cards) {
        return cards.stream().distinct().anyMatch(kind ->
                cards.stream().allMatch(card -> card.equals(kind) || card.substitutesFor(kind)));
    }

    private CardEffect single(CardType type) throws InvalidMoveException {
        CardEffect effect = cardEffects.get(type);
        if (effect == null) {
            throw new InvalidMoveException(type.displayName() + " can't be played on its own");
        }
        return effect;
    }
}
