package ek.ui;

import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.ui.Style.Tone;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The box drawn at the start of the viewer's turn: deck and opponents, the last events,
 * and the hand in two columns. Pure text: no I/O.
 *
 * <p>Every row is padded on plain text before it is painted, so color codes never
 * change the box's width.
 */
final class Dashboard {

    /** Width of the whole box, borders included. */
    static final int WIDTH = 60;
    private static final int INNER = WIDTH - 4;
    private static final int COLUMN = INNER / 2;

    private final Style style;

    Dashboard(Style style) {
        this.style = style;
    }

    List<String> render(GameView view, HandMenu hand, List<ConsoleOutput.Line> recent) {
        List<String> rows = new ArrayList<>();
        rows.add(border('┌', "Exploding Kittens", "Deck " + view.deckSize(), '┐'));
        wrap(view.opponents().stream().map(Dashboard::opponent).toList(), " │ ")
                .forEach(text -> rows.add(row(text, Tone.NORMAL)));
        if (!view.faceUpPositions().isEmpty()) {
            String positions = view.faceUpPositions().stream().map(p -> ordinal(p + 1))
                    .collect(Collectors.joining(", "));
            wrap(List.of(("Face up in the deck: " + positions + " from the top").split(" ")), " ")
                    .forEach(text -> rows.add(row(text, Tone.DANGER)));
        }
        rows.add(border('├', "Recent", "", '┤'));
        if (recent.isEmpty()) {
            rows.add(row("(nothing yet)", Tone.MUTED));
        }
        for (ConsoleOutput.Line line : recent) {
            wrap(List.of(line.text().split(" ")), " ").forEach(text -> rows.add(row(text, line.tone())));
        }
        rows.add(border('├', "Your hand" + (view.turnsLeft() > 1 ? " · " + view.turnsLeft() + " turns to take" : ""),
                "", '┤'));
        handRows(hand).forEach(text -> rows.add(row(text, Tone.NORMAL)));
        rows.add("└" + "─".repeat(WIDTH - 2) + "┘");
        return rows;
    }

    /** 1st, 2nd, 3rd, 4th, …, 11th, 12th, 13th, 21st. */
    static String ordinal(int n) {
        int lastTwo = n % 100;
        String suffix = lastTwo >= 11 && lastTwo <= 13 ? "th" : switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        return n + suffix;
    }

    private static String opponent(OpponentView o) {
        return o.name() + " " + (o.alive() ? o.handSize() : "✗");
    }

    /** The hand entries in two columns, filled top to bottom: 1–3 left, 4–5 right. */
    private static List<String> handRows(HandMenu hand) {
        List<String> entries = hand.lines().stream().map(String::strip).toList();
        if (entries.isEmpty()) {
            return List.of("(no cards)");
        }
        int height = (entries.size() + 1) / 2;
        List<String> rows = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            String left = entries.get(i);
            rows.add(i + height < entries.size() ? pad(left, COLUMN) + entries.get(i + height) : left);
        }
        return rows;
    }

    /** "┌ title ───── right ┐", exactly {@link #WIDTH} wide. */
    private String border(char start, String title, String right, char end) {
        String left = start + " " + title + " ";
        String tail = right.isEmpty() ? String.valueOf(end) : " " + right + " " + end;
        String fill = "─".repeat(Math.max(1, WIDTH - left.length() - tail.length()));
        return style.paint(left, Tone.HEADER) + fill + style.paint(tail.substring(0, tail.length() - 1), Tone.HEADER)
                + end;
    }

    private String row(String text, Tone tone) {
        return "│ " + style.paint(pad(text, INNER), tone) + " │";
    }

    private static String pad(String text, int width) {
        return text.length() >= width ? text : text + " ".repeat(width - text.length());
    }

    /**
     * Joins {@code pieces} with {@code separator} into lines of at most {@link #INNER}
     * characters, breaking only between pieces. A piece longer than a line is cut into parts.
     */
    static List<String> wrap(List<String> pieces, String separator) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String piece : pieces) {
            for (int i = 0; i < piece.length(); i += INNER) {
                String part = piece.substring(i, Math.min(piece.length(), i + INNER));
                if (!line.isEmpty() && line.length() + separator.length() + part.length() > INNER) {
                    lines.add(line.toString());
                    line.setLength(0);
                }
                line.append(line.isEmpty() ? "" : separator).append(part);
            }
        }
        if (!line.isEmpty()) {
            lines.add(line.toString());
        }
        return lines;
    }
}
