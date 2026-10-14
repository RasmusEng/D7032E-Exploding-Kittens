package ek.ui;

import ek.ui.Style.Tone;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Function;

/**
 * The console's output, shared by {@link ConsoleView} and {@link ConsoleController}.
 * It keeps the last few event lines for the dashboard's "Recent" section, and works in
 * one of two modes, chosen by the {@link Style}:
 * <ul>
 *   <li><b>Redraw</b> (ANSI): the screen is one frame — board, turn heading, and the
 *       question being asked (its lines and prompt) — redrawn in place after every change,
 *       so the terminal never scrolls. Events appear in the board's "Recent" box, and an
 *       answered question disappears, so lines never pile up under the board.</li>
 *   <li><b>Plain</b>: lines are appended. It remembers whether a prompt is waiting at the
 *       end of the line, so a line printed meanwhile starts on a new line.</li>
 * </ul>
 *
 * <p><b>Thread-safety.</b> All methods are synchronized: the engine thread prints events
 * while a Nope-window thread may be showing the Nope prompt.
 */
public final class ConsoleOutput {

    /** How many event lines the dashboard's "Recent" section shows. */
    static final int HISTORY = 8;

    /** In redraw mode, how many lines of the current question are shown; older ones drop off. */
    static final int BODY_LIMIT = 15;

    /** One remembered event line. */
    record Line(String text, Tone tone) {
    }

    private final PrintStream out;
    private final Style style;
    private final Deque<Line> recent = new ArrayDeque<>();
    private long promptCount;
    private long openPrompt;          // id of the open prompt, 0 if none

    // the frame, used in redraw mode only
    private Function<List<Line>, List<String>> board;   // null until the first board
    private String heading = "";
    private final Deque<String> body = new ArrayDeque<>();
    private String promptText = "";

    /** Plain output, without colours. */
    public ConsoleOutput(PrintStream out) {
        this(out, Style.PLAIN);
    }

    public ConsoleOutput(PrintStream out, Style style) {
        this.out = out;
        this.style = style;
    }

    Style style() {
        return style;
    }

    /** Prints a whole line. */
    public synchronized void println(String line) {
        println(line, Tone.NORMAL);
    }

    /** Prints a whole line in the colour of {@code tone}, without remembering it. */
    public synchronized void println(String line, Tone tone) {
        if (redraws()) {
            addToBody(style.paint(line, tone));
            redraw(false);
        } else {
            endOpenPrompt();
            out.println(style.paint(line, tone));
        }
    }

    /** Shows an event line and remembers it for the dashboard's "Recent" section. */
    public synchronized void event(String line, Tone tone) {
        recent.addLast(new Line(line, tone));
        if (recent.size() > HISTORY) {
            recent.removeFirst();
        }
        if (redraws()) {
            redraw(false);
        } else {
            endOpenPrompt();
            out.println(style.paint(line, tone));
        }
    }

    /** The last {@link #HISTORY} event lines, oldest first. */
    synchronized List<Line> recent() {
        return List.copyOf(recent);
    }

    /** A new turn: the heading under the board (redraw), or a separator line (plain). */
    public synchronized void heading(String text) {
        if (redraws()) {
            heading = text;
            body.clear();
            redraw(false);
        } else {
            println("");
            println(text, Tone.HEADER);
        }
    }

    /**
     * Shows a fresh board: {@code render} turns the recent event lines into its rows.
     * In redraw mode it replaces the old board and the lines under it, and is redrawn
     * with every later event; in plain mode it is printed once.
     */
    public synchronized void showBoard(Function<List<Line>, List<String>> render) {
        if (redraws()) {
            board = render;
            body.clear();
            redraw(true);
        } else {
            render.apply(recent()).forEach(this::println);
        }
    }

    /** Like {@link #showBoard}, but in plain mode prints nothing, to avoid a board per question. */
    public synchronized void updateBoard(Function<List<Line>, List<String>> render) {
        if (redraws()) {
            showBoard(render);
        }
    }

    /**
     * Prints {@code text} and leaves the cursor after it for the answer.
     *
     * @return an id for {@link #answered}, {@link #updatePrompt} and {@link #closePrompt}
     */
    public synchronized long prompt(String text) {
        if (redraws()) {
            promptText = text;            // replaces any open prompt
            redraw(false);
        } else {
            endOpenPrompt();
            out.print(text);
            out.flush();
        }
        openPrompt = ++promptCount;
        return openPrompt;
    }

    /**
     * Changes the text of a prompt that is still open, e.g. its countdown. In redraw mode only
     * the prompt's line is rewritten, not the frame, so what the player is typing after it
     * stays; later redraws show the new text. Plain mode prints nothing: the prompt was
     * printed once, and rewriting it could overwrite a line printed after it.
     * The new text should be as long as the old one, which it overwrites.
     */
    public synchronized void updatePrompt(long prompt, String text) {
        if (openPrompt == prompt && redraws()) {
            promptText = text;
            out.print(style.rewriteLine(text));
            out.flush();
        }
    }

    /** The user answered the prompt with {@code answer}; the terminal has already moved to a new line. */
    public synchronized void answered(long prompt, String answer) {
        if (openPrompt == prompt) {
            closeWith(answer);
        }
    }

    /**
     * Ends the prompt with {@code text} if it is still open; in plain mode, if anything was
     * printed after it, it has scrolled away and the text would be out of place.
     */
    public synchronized void closePrompt(long prompt, String text) {
        if (openPrompt == prompt) {
            if (!redraws()) {
                out.println(text);
            }
            closeWith(text);
        }
    }

    /**
     * Closes the open prompt. In redraw mode the question is over, so it and the lines
     * printed for it (a menu, an error) are removed: only the board stays on screen.
     */
    private void closeWith(String answer) {
        openPrompt = 0;
        if (redraws()) {
            body.clear();
            promptText = "";
            redraw(false);
        }
    }

    private boolean redraws() {
        return style.canRewriteLines();
    }

    private void endOpenPrompt() {
        if (openPrompt != 0) {
            out.println();
            openPrompt = 0;
        }
    }

    private void addToBody(String line) {
        body.addLast(line);
        if (body.size() > BODY_LIMIT) {
            body.removeFirst();
        }
    }

    /**
     * Writes the whole frame from the top left in one go. Each line erases what is left of
     * the old one, and the rest of the screen is erased after the prompt, which leaves the
     * cursor where the answer is typed. {@code clear} also wipes the screen and scrollback.
     */
    private void redraw(boolean clear) {
        List<String> rows = new ArrayList<>(board != null ? board.apply(recent())
                : recent.stream().map(l -> style.paint(l.text(), l.tone())).toList());
        if (!heading.isEmpty()) {
            rows.add(style.paint(heading, Tone.HEADER));
        }
        rows.addAll(body);
        StringBuilder frame = new StringBuilder(clear ? style.clearScreen() : style.home());
        rows.forEach(row -> frame.append(row).append(style.eraseToEndOfLine()).append(System.lineSeparator()));
        frame.append(promptText).append(style.eraseBelow());
        out.print(frame);
        out.flush();      // System.out flushes by itself, but the stream passed in may buffer
    }
}
