package ek.ui;

import ek.model.Card;
import ek.model.CardType;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.ui.Style.Tone;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class DashboardTest {

    private static final List<OpponentView> OPPONENTS =
            List.of(new OpponentView("Bob", 3, true), new OpponentView("Cid", 0, false));

    private static List<String> render(Style style, int turnsLeft, List<ConsoleOutput.Line> recent, CardType... hand) {
        List<Card> cards = Arrays.stream(hand).map(Card::new).toList();
        GameView view = new GameView(cards, OPPONENTS, 23, turnsLeft);
        return new Dashboard(style).render(view, new HandMenu(cards), recent);
    }

    private static List<String> render(CardType... hand) {
        return render(Style.PLAIN, 1, List.of(), hand);
    }

    @Test
    void showsWhereFaceUpCardsAreInTheDeck() {
        GameView view = new GameView(List.of(), OPPONENTS, 23, 1, List.of(0, 2));
        List<String> rows = new Dashboard(Style.PLAIN).render(view, new HandMenu(List.of()), List.of());
        assertTrue(rows.stream().anyMatch(r -> r.contains("Face up in the deck: 1st, 3rd from the top")),
                String.join("\n", rows));
        rows.forEach(row -> assertEquals(Dashboard.WIDTH, row.length(), row));
    }

    @Test
    void noFaceUpCards_noFaceUpRow() {
        assertTrue(render(SKIP).stream().noneMatch(r -> r.contains("Face up")));
    }

    @Test
    void everyRowIsTheSameWidth() {
        List<ConsoleOutput.Line> recent = List.of(new ConsoleOutput.Line("Bob played Attack.", Tone.NORMAL));
        render(Style.PLAIN, 2, recent, ATTACK, TACO_CAT, TACO_CAT, NOPE, SKIP)
                .forEach(row -> assertEquals(Dashboard.WIDTH, row.length(), row));
    }

    @Test
    void showsDeckAndOpponents_explodedMarked() {
        String box = String.join("\n", render(SKIP));
        assertTrue(box.contains("Deck 23"));
        assertTrue(box.contains("Bob 3 │ Cid ✗"));
    }

    @Test
    void handInTwoColumns_groupedLikeTheMenu() {
        List<String> rows = render(ATTACK, DEFUSE, FAVOR, NOPE, TACO_CAT, TACO_CAT);
        // sorted by name: 1) Attack 2) Defuse 3) Favor | 4) Nope 5) Taco Cat ×2
        assertTrue(rows.stream().anyMatch(r -> r.contains("1) Attack") && r.contains("4) Nope")));
        assertTrue(rows.stream().anyMatch(r -> r.contains("2) Defuse") && r.contains("5) Taco Cat ×2")));
        assertTrue(rows.stream().anyMatch(r -> r.contains("3) Favor")));
    }

    @Test
    void turnsToTake_shownOnlyWhenMoreThanOne() {
        assertTrue(String.join("\n", render(Style.PLAIN, 2, List.of(), SKIP)).contains("2 turns to take"));
        assertFalse(String.join("\n", render(Style.PLAIN, 1, List.of(), SKIP)).contains("turns to take"));
    }

    @Test
    void longRecentLine_isWrappedNotCut() {
        String reason = "word ".repeat(20).strip();
        List<String> rows = render(Style.PLAIN, 1, List.of(new ConsoleOutput.Line(reason, Tone.DANGER)), SKIP);
        long wordsShown = rows.stream().mapToLong(r -> r.split("word", -1).length - 1).sum();
        assertEquals(20, wordsShown);
    }

    @Test
    void emptyHandAndHistory_saySo() {
        String box = String.join("\n", render());
        assertTrue(box.contains("(no cards)"));
        assertTrue(box.contains("(nothing yet)"));
    }

    @Test
    void plainStyle_hasNoEscapeCodes_ansiStyleHas() {
        List<ConsoleOutput.Line> recent = List.of(new ConsoleOutput.Line("BOOM!", Tone.DANGER));
        assertFalse(String.join("", render(Style.PLAIN, 1, recent, SKIP)).contains("\033"));
        assertTrue(String.join("", render(Style.ANSI, 1, recent, SKIP)).contains("\033[1;31m"));
    }
}
