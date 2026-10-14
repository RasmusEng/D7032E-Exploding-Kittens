package ek.ui;

import ek.expansion.BaseGame;
import ek.expansion.Expansion;
import ek.expansion.GameVariant;
import ek.expansion.OneKittenLessExpansion;
import ek.expansion.TestOnlyExpansion;
import ek.expansion.TestOnlyVariant;
import ek.rules.GameConfig;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleMenuTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final Expansion oneKittenLess = new OneKittenLessExpansion();
    private final Expansion testOnly = new TestOnlyExpansion();
    private final GameVariant variant = new TestOnlyVariant();

    private GameConfig ask(String input) {
        return ask(input, List.of(oneKittenLess, testOnly), List.of(variant), List.of());
    }

    private GameConfig ask(String input, List<Expansion> expansions, List<GameVariant> variants, List<String> warnings) {
        return new ConsoleMenu(new BufferedReader(new StringReader(input)),
                new ConsoleOutput(new PrintStream(output, true))).ask(expansions, variants, warnings);
    }

    private String printed() {
        return output.toString();
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
    }

    @Test
    void defaultPath_enterForNoExpansionsAndNoVariants_givesTheBaseGame() {
        GameConfig config = ask("\n\n2\n0\n");
        assertEquals(1, config.expansions().size());
        assertInstanceOf(BaseGame.class, config.expansions().getFirst());
        assertTrue(config.variants().isEmpty());
        assertEquals(2, config.humans());
        assertEquals(0, config.bots());
    }

    @Test
    void choosingExpansionsAndAVariant() {
        GameConfig config = ask("2 1\n1\n1\n2\n");
        assertEquals(List.of(BaseGame.class, OneKittenLessExpansion.class, TestOnlyExpansion.class),
                config.expansions().stream().map(Object::getClass).toList());
        assertEquals(List.of(variant), config.variants());
        assertEquals(1, config.humans());
        assertEquals(2, config.bots());
    }

    @Test
    void choosingOneOfTwoExpansions_leavesTheOtherOut() {
        GameConfig config = ask("2\n\n2\n0\n");
        assertEquals(List.of(BaseGame.class, TestOnlyExpansion.class),
                config.expansions().stream().map(Object::getClass).toList());
    }

    @Test
    void expansionsAndVariants_areAskedBeforePlayerCounts() {
        ask("\n\n2\n0\n");
        String text = printed();
        int expansions = text.indexOf("Expansions");
        int variants = text.indexOf("Variants");
        int humans = text.indexOf("Human players");
        int bots = text.indexOf("Bots");
        assertTrue(expansions >= 0 && expansions < variants && variants < humans && humans < bots, text);
    }

    @Test
    void showsTheNamesAndDescriptionsTheOptionsGiveThemselves() {
        ask("\n\n2\n0\n");
        assertTrue(printed().contains("1) One kitten less"), printed());
        assertTrue(printed().contains("2) Test-only expansion: Adds 3 Test Cards."), printed());
        assertTrue(printed().contains("1) Test-only variant: Removes the top card after dealing."), printed());
    }

    @Test
    void showsTheValidPlayerRange() {
        ask("\n\n2\n0\n");
        assertTrue(printed().contains("2–5 players in total"), printed());
    }

    @Test
    void invalidSelection_askedAgain() {
        // not a number, out of range, chosen twice, then a valid choice
        GameConfig config = ask("x\n3\n1 1\n1\n\n2\n0\n");
        assertEquals(2, config.expansions().size());
        assertEquals(3, count(printed(), "Enter numbers from 1 to 2"));
    }

    @Test
    void invalidCount_askedAgain() {
        GameConfig config = ask("\n\nabc\n\n-1\n2\n0\n");
        assertEquals(2, config.humans());
        assertEquals(3, count(printed(), "Enter a whole number"));
    }

    @Test
    void rule01_onePlayerInTotal_rejectedAndCountsAskedAgain() {
        GameConfig config = ask("\n\n1\n0\n1\n1\n");
        assertEquals(1, config.humans());
        assertEquals(1, config.bots());
        assertTrue(printed().contains("Need 2–5 players in total, got 1"), printed());
    }

    @Test
    void rule01_sixPlayersInTotal_rejectedAndCountsAskedAgain() {
        GameConfig config = ask("\n\n5\n1\n5\n0\n");
        assertEquals(5, config.humans());
        assertEquals(0, config.bots());
        assertTrue(printed().contains("Need 2–5 players in total, got 6"), printed());
    }

    @Test
    void noHumans_rejectedAndAskedAgain() {
        GameConfig config = ask("\n\n0\n2\n1\n1\n");
        assertEquals(1, config.humans());
        assertTrue(printed().contains("at least 1 human"), printed());
    }

    @Test
    void nothingInstalled_noSelectionQuestions() {
        GameConfig config = ask("2\n0\n", List.of(), List.of(), List.of());
        assertEquals(2, config.humans());
        assertTrue(printed().contains("No expansions installed"), printed());
        assertTrue(printed().contains("No variants installed"), printed());
    }

    @Test
    void loadingProblems_areShown() {
        ask("2\n0\n", List.of(), List.of(), List.of("Skipped a broken expansion: Provider x.Y not found"));
        assertTrue(printed().contains("Provider x.Y not found"), printed());
    }

    @Test
    void inputClosed_throws() {
        assertThrows(IllegalStateException.class, () -> ask("\n\n2\n"));
    }
}
