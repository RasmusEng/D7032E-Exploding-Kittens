package ek.ui;

import ek.model.Card;
import ek.model.CardType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A hand shown as a numbered menu with equal cards grouped ("2) Taco Cat ×2"), sorted by
 * name, and the parsing of what the player types into it. Pure text: no I/O.
 */
final class HandMenu {

    private record Entry(CardType type, int count) {
    }

    private final List<Card> hand;
    private final List<Entry> entries;

    HandMenu(List<Card> hand) {
        this.hand = List.copyOf(hand);
        Map<CardType, Integer> counts = new LinkedHashMap<>();
        hand.forEach(c -> counts.merge(c.type(), 1, Integer::sum));
        this.entries = counts.entrySet().stream()
                .map(e -> new Entry(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(e -> e.type().displayName()))
                .toList();
    }

    /** Number of menu entries (distinct card types). */
    int size() {
        return entries.size();
    }

    /** One line per entry, numbered from 1. */
    List<String> lines() {
        List<String> lines = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            lines.add("  " + (i + 1) + ") " + e.type().displayName() + (e.count() > 1 ? " ×" + e.count() : ""));
        }
        return lines;
    }

    /** One line per entry with the card's rules text. */
    List<String> descriptions() {
        return entries.stream()
                .map(e -> "  " + e.type().displayName() + ": " + e.type().description())
                .toList();
    }

    /**
     * Parses entry numbers, e.g. "2" for one card or "3 3" for a pair. A number may be
     * repeated as often as the hand has that card.
     */
    Optional<List<CardType>> parsePlay(String line) {
        List<CardType> cards = new ArrayList<>();
        Map<Integer, Integer> used = new LinkedHashMap<>();
        for (String token : line.trim().split("\\s+")) {
            Optional<Integer> n = ConsoleController.parseInt(token);
            if (n.isEmpty() || n.get() < 1 || n.get() > entries.size()) {
                return Optional.empty();
            }
            Entry entry = entries.get(n.get() - 1);
            if (used.merge(n.get(), 1, Integer::sum) > entry.count()) {
                return Optional.empty();
            }
            cards.add(entry.type());
        }
        return Optional.of(cards);
    }

    /** The position in the original hand of a card from entry {@code number} (1-based). */
    int handIndexOf(int number) {
        CardType type = entries.get(number - 1).type();
        for (int i = 0; i < hand.size(); i++) {
            if (hand.get(i).type().equals(type)) {
                return i;
            }
        }
        throw new IllegalStateException("No " + type.displayName() + " in hand");
    }
}
