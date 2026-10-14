package ek.ui;

import java.util.Map;

/**
 * How console text is coloured: {@link #ANSI} escape codes, or {@link #PLAIN} text for
 * terminals without colour and for tests. Chosen once in the composition root, so the
 * rest of the UI never asks what kind of terminal it runs in.
 */
public final class Style {

    /** What a line means; each style decides how that looks. */
    public enum Tone { NORMAL, HEADER, MUTED, GOOD, DANGER, NOPE }

    /** No escape codes at all: colours are dropped and the screen is never cleared. */
    public static final Style PLAIN = new Style(false);

    /** ANSI colours, and the screen redrawn in place. */
    public static final Style ANSI = new Style(true);

    private static final String ESC = "\033[";
    private static final String RESET = ESC + "0m";

    private final boolean ansi;

    private Style(boolean ansi) {
        this.ansi = ansi;
    }

    /** {@link #PLAIN} if the environment asks for no colour (NO_COLOR, or TERM=dumb), else {@link #ANSI}. */
    public static Style detect(Map<String, String> env) {
        return env.containsKey("NO_COLOR") || "dumb".equals(env.get("TERM")) ? PLAIN : ANSI;
    }

    /** {@code text} in the colour of {@code tone}; unchanged in plain style. */
    public String paint(String text, Tone tone) {
        if (!ansi || tone == Tone.NORMAL) {
            return text;
        }
        String code = switch (tone) {
            case HEADER -> "1;36";
            case MUTED -> "2";
            case GOOD -> "32";
            case DANGER -> "1;31";
            case NOPE -> "33";
            case NORMAL -> "0";
        };
        return ESC + code + "m" + text + RESET;
    }

    /** Moves to the top left and clears the screen and the scrollback; empty in plain style. */
    String clearScreen() {
        return ansi ? ESC + "H" + ESC + "2J" + ESC + "3J" : "";
    }

    /** Whether the screen can be redrawn in place; plain style appends lines instead. */
    boolean canRewriteLines() {
        return ansi;
    }

    /** Moves to the top left without clearing; only valid if {@link #canRewriteLines()}. */
    String home() {
        return ESC + "H";
    }

    /** Erases the rest of the current line; only valid if {@link #canRewriteLines()}. */
    String eraseToEndOfLine() {
        return ESC + "K";
    }

    /**
     * Writes {@code text} over the start of the cursor's line and puts the cursor back where it
     * was, so anything typed after it stays. Only valid if {@link #canRewriteLines()}.
     */
    String rewriteLine(String text) {
        return "\0337\r" + text + "\0338";      // save cursor, start of line, text, restore cursor
    }

    /** Erases from the cursor to the end of the screen; only valid if {@link #canRewriteLines()}. */
    String eraseBelow() {
        return ESC + "J";
    }
}
