package ek.app;

import ek.expansion.BaseGame;
import ek.expansion.ExpansionRegistry;
import ek.rules.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommandLineTest {

    @Test
    void server_opensTheMenu() {
        assertEquals(new CommandLine.OpenMenu(), CommandLine.parse("server"));
    }

    @Test
    void serverWithCounts_isABaseGameOnlyShortcut_evenWithExpansionsInstalled() {
        // the test classpath registers optional expansions...
        assertFalse(new ExpansionRegistry().optionalExpansions().isEmpty());
        // ...but the shortcut never offers them
        var host = assertInstanceOf(CommandLine.HostBaseGame.class, CommandLine.parse("server", "2", "1"));
        GameConfig config = host.config();
        assertEquals(2, config.humans());
        assertEquals(1, config.bots());
        assertEquals(1, config.expansions().size());
        assertInstanceOf(BaseGame.class, config.expansions().getFirst());
        assertTrue(config.variants().isEmpty());
    }

    @Test
    void rule01_shortcut_oneOrSixPlayers_invalid_withTheConfigsMessage() {
        var one = assertInstanceOf(CommandLine.Invalid.class, CommandLine.parse("server", "1", "0"));
        var six = assertInstanceOf(CommandLine.Invalid.class, CommandLine.parse("server", "5", "1"));
        assertTrue(one.message().contains("2–5"), one.message());
        assertTrue(six.message().contains("2–5"), six.message());
    }

    @Test
    void shortcut_notNumbers_invalid() {
        assertInstanceOf(CommandLine.Invalid.class, CommandLine.parse("server", "two", "1"));
        assertInstanceOf(CommandLine.Invalid.class, CommandLine.parse("server", "2", "-1"));
    }

    @Test
    void shortcut_takesNoExpansionArguments() {
        assertInstanceOf(CommandLine.Invalid.class, CommandLine.parse("server", "2", "1", "some-expansion"));
    }

    @Test
    void client_joinsTheGivenHost() {
        assertEquals(new CommandLine.Join("10.0.0.2"), CommandLine.parse("client", "10.0.0.2"));
    }

    @Test
    void anythingElse_invalid() {
        assertInstanceOf(CommandLine.Invalid.class, CommandLine.parse());
        assertInstanceOf(CommandLine.Invalid.class, CommandLine.parse("client"));
        assertInstanceOf(CommandLine.Invalid.class, CommandLine.parse("play", "2", "1"));
    }
}
