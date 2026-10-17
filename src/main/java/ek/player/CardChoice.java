package ek.player;

import ek.model.Card;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A decision about cards, asked through {@link PlayerController#chooseCards}: pick
 * {@code picks} of the {@code options}, in order. Picking all of them puts them in a new
 * order (Alter the Future); picking fewer selects some (e.g. a card to give up).
 *
 * @param prompt  what the choice is for, shown to the player; display text only, never parsed
 * @param options the cards to choose from, in the order shown
 * @param picks   how many to pick, from 1 to {@code options.size()}
 */
public record CardChoice(String prompt, List<Card> options, int picks) implements Serializable {

    public CardChoice {
        Objects.requireNonNull(prompt, "prompt");
        options = List.copyOf(options);
        if (picks < 1 || picks > options.size()) {
            throw new IllegalArgumentException("Can't pick " + picks + " of " + options.size() + " cards");
        }
    }

    /** Put all of {@code cards} in an order. */
    public static CardChoice orderOf(String prompt, List<Card> cards) {
        return new CardChoice(prompt, cards, cards.size());
    }

    /** Whether {@code answer} is exactly {@link #picks()} different indexes into {@link #options()}. */
    public boolean accepts(List<Integer> answer) {
        if (answer == null || answer.size() != picks) {
            return false;
        }
        Set<Integer> seen = new HashSet<>();
        for (Integer index : answer) {
            if (index == null || index < 0 || index >= options.size() || !seen.add(index)) {
                return false;
            }
        }
        return true;
    }
}