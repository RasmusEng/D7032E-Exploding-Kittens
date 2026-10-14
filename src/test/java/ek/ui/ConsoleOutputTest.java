package ek.ui;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleOutputTest {

    private static final String NL = System.lineSeparator();

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final ConsoleOutput out = new ConsoleOutput(new PrintStream(output, true));

    @Test
    void lineAfterAnOpenPrompt_startsOnANewLine() {
        out.prompt("Nope? ");
        out.println("Bob drew a card.");
        assertEquals("Nope? " + NL + "Bob drew a card." + NL, output.toString());
    }

    @Test
    void answeredPrompt_needsNoExtraNewLine() {
        long prompt = out.prompt("Card: ");
        out.answered(prompt, "ok");           // the user's Enter ended the line
        out.println("ok");
        assertEquals("Card: ok" + NL, output.toString());
    }

    @Test
    void closePrompt_endsItsOwnLine() {
        long prompt = out.prompt("Nope? ");
        out.closePrompt(prompt, "(time's up)");
        assertEquals("Nope? (time's up)" + NL, output.toString());
    }

    @Test
    void closePrompt_afterOtherOutput_printsNothing() {
        long prompt = out.prompt("Nope? ");
        out.println("Bob drew a card.");
        out.closePrompt(prompt, "(time's up)");
        assertEquals("Nope? " + NL + "Bob drew a card." + NL, output.toString());
    }

    @Test
    void closePrompt_doesNotEndANewerPrompt() {
        long old = out.prompt("Nope? ");
        out.prompt("Play: ");
        out.closePrompt(old, "(time's up)");
        assertEquals("Nope? " + NL + "Play: ", output.toString());
    }

    @Test
    void updatePrompt_ansi_rewritesOnlyThePromptLine() {
        ConsoleOutput ansi = new ConsoleOutput(new PrintStream(output, true), Style.ANSI);
        long prompt = ansi.prompt("Nope? 5");
        output.reset();

        ansi.updatePrompt(prompt, "Nope? 4");
        // save cursor, start of line, new text, restore cursor: what was typed after it stays
        assertEquals("\0337\rNope? 4\0338", output.toString());
    }

    @Test
    void updatePrompt_ansi_laterRedrawShowsTheNewText() {
        ConsoleOutput ansi = new ConsoleOutput(new PrintStream(output, true), Style.ANSI);
        long prompt = ansi.prompt("Nope? 5");
        ansi.updatePrompt(prompt, "Nope? 4");
        output.reset();

        ansi.event("Bob drew a card.", Style.Tone.NORMAL);
        assertTrue(output.toString().contains("Nope? 4"));
        assertFalse(output.toString().contains("Nope? 5"));
    }

    @Test
    void updatePrompt_plain_printsNothing() {
        long prompt = out.prompt("Nope? (5 s): ");
        output.reset();
        out.updatePrompt(prompt, "Nope? 4");
        assertEquals("", output.toString());
    }

    @Test
    void updatePrompt_afterTheAnswer_printsNothing() {
        ConsoleOutput ansi = new ConsoleOutput(new PrintStream(output, true), Style.ANSI);
        long prompt = ansi.prompt("Nope? 5");
        ansi.answered(prompt, "");
        output.reset();
        ansi.updatePrompt(prompt, "Nope? 4");
        assertEquals("", output.toString());
    }

    @Test
    void history_keepsOnlyTheLastEventLines() {
        for (int i = 1; i <= ConsoleOutput.HISTORY + 2; i++) {
            out.event("line " + i, Style.Tone.NORMAL);
        }
        List<ConsoleOutput.Line> recent = out.recent();
        assertEquals(ConsoleOutput.HISTORY, recent.size());
        assertEquals("line 3", recent.get(0).text());
        assertEquals("line " + (ConsoleOutput.HISTORY + 2), recent.get(recent.size() - 1).text());
    }

    @Test
    void plainLines_areNotKept() {
        out.println("header", Style.Tone.HEADER);
        out.println("prompt text");
        assertTrue(out.recent().isEmpty());
    }

    @Test
    void heading_plain_isASeparatorLine() {
        out.heading("=== Bob's turn ===");
        assertEquals(NL + "=== Bob's turn ===" + NL, output.toString());
    }

    @Test
    void showBoard_plain_printsItOnce_andUpdateBoardPrintsNothing() {
        out.showBoard(recent -> List.of("BOARD"));
        out.updateBoard(recent -> List.of("BOARD"));
        assertEquals("BOARD" + NL, output.toString());
    }

    // ---- redraw mode (ANSI): the whole frame is rewritten in place, nothing scrolls ----

    private final ConsoleOutput ansi = new ConsoleOutput(new PrintStream(output, true), Style.ANSI);

    /** The rows of the last frame drawn, without escape codes; the last row is the open prompt. */
    private List<String> lastFrame() {
        String all = output.toString();
        String frame = all.substring(all.lastIndexOf("\033[H") + 3).replaceAll("\033\\[[0-9;]*[A-Za-z]", "");
        return List.of(frame.split(NL, -1));
    }

    @Test
    void redraw_showBoard_clearsTheScreenAndTheScrollback() {
        ansi.showBoard(recent -> List.of("BOARD"));
        assertTrue(output.toString().startsWith("\033[H\033[2J\033[3J"));
        assertEquals(List.of("BOARD", ""), lastFrame());
    }

    @Test
    void redraw_eventIsShownInTheBoard_notPrintedBelowIt() {
        ansi.showBoard(recent -> List.of("BOARD " + recent.size()));
        ansi.event("Bob drew a card.", Style.Tone.NORMAL);
        assertEquals(List.of("BOARD 1", ""), lastFrame());
    }

    @Test
    void redraw_beforeAnyBoard_showsTheRecentEvents() {
        ansi.event("Bob drew a card.", Style.Tone.NORMAL);
        assertEquals(List.of("Bob drew a card.", ""), lastFrame());
    }

    @Test
    void redraw_promptStaysOnTheLastRow_whileLinesAreAdded() {
        ansi.showBoard(recent -> List.of("BOARD"));
        ansi.prompt("Nope? ");
        ansi.println("Bob played Nope!");
        assertEquals(List.of("BOARD", "Bob played Nope!", "Nope? "), lastFrame());
    }

    @Test
    void redraw_answeredQuestion_disappears_leavingOnlyTheBoard() {
        ansi.showBoard(recent -> List.of("BOARD"));
        ansi.println("Choose a player:");
        ansi.answered(ansi.prompt("Player number: "), "1");
        assertEquals(List.of("BOARD", ""), lastFrame());
    }

    @Test
    void redraw_closedPrompt_disappears() {
        long prompt = ansi.prompt("Nope? ");
        ansi.closePrompt(prompt, "(time's up)");
        assertEquals(List.of(""), lastFrame());
    }

    @Test
    void redraw_newHeading_replacesTheOldOneAndItsLines() {
        ansi.heading("=== Bob's turn ===");
        ansi.println("old line");
        ansi.heading("=== Your turn ===");
        assertEquals(List.of("=== Your turn ===", ""), lastFrame());
    }

    @Test
    void redraw_newBoard_dropsTheLinesUnderTheOldOne() {
        ansi.showBoard(recent -> List.of("OLD"));
        ansi.println("Choose a player:");
        ansi.updateBoard(recent -> List.of("NEW"));
        assertEquals(List.of("NEW", ""), lastFrame());
    }

    @Test
    void redraw_keepsOnlyTheLastLinesUnderTheBoard() {
        for (int i = 1; i <= ConsoleOutput.BODY_LIMIT + 3; i++) {
            ansi.println("line " + i);
        }
        List<String> frame = lastFrame();
        assertEquals(ConsoleOutput.BODY_LIMIT + 1, frame.size());
        assertEquals("line 4", frame.get(0));
    }
}
