package ek.ui;

import ek.ui.Style.Tone;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StyleTest {

    @Test
    void plain_leavesTextAlone_andNeverClears() {
        assertEquals("BOOM!", Style.PLAIN.paint("BOOM!", Tone.DANGER));
        assertEquals("", Style.PLAIN.clearScreen());
        assertFalse(Style.PLAIN.canRewriteLines());
    }

    @Test
    void ansi_wrapsTextInAColourAndResets() {
        assertEquals("\033[1;31mBOOM!\033[0m", Style.ANSI.paint("BOOM!", Tone.DANGER));
        assertEquals("plain", Style.ANSI.paint("plain", Tone.NORMAL));
        assertEquals("\033[H\033[2J\033[3J", Style.ANSI.clearScreen());
    }

    @Test
    void detect_respectsNoColourAndDumbTerminals() {
        assertSame(Style.PLAIN, Style.detect(Map.of("NO_COLOR", "1", "TERM", "xterm")));
        assertSame(Style.PLAIN, Style.detect(Map.of("TERM", "dumb")));
        assertSame(Style.ANSI, Style.detect(Map.of("TERM", "xterm-256color")));
    }
}
