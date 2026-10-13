package ek.ui;

import ek.model.Card;
import ek.model.CardType;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.player.HandChange;
import ek.player.NopeOffer;
import ek.player.TurnAction;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleControllerTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private ConsoleController controller(String input) {
        return new ConsoleController(new BufferedReader(new StringReader(input)),
                new ConsoleOutput(new PrintStream(output, true)), List.of(DEFUSE, NOPE, TACO_CAT));
    }

    private static GameView view(CardType... hand) {
        List<Card> cards = java.util.Arrays.stream(hand).map(Card::new).toList();
        return new GameView(cards, List.of(new OpponentView("Bob", 3, true)), 10, 1);
    }

    private String printed() {
        return output.toString();
    }

    /** Fake offer: open until closed; records whether Nope was played. */
    private static class FakeOffer implements NopeOffer {
        boolean open = true;
        boolean noped;

        public String playedBy() { return "Bob"; }
        public List<CardType> cards() { return List.of(ATTACK); }
        public int nopesSoFar() { return 0; }
        public Duration window() { return Duration.ofSeconds(5); }
        public boolean isOpen() { return open; }
        public boolean playNope() {
            noped = open;
            return open;
        }
    }

    @Test
    void chooseAction_d_passes() {
        assertEquals(new TurnAction.Pass(), controller("d\n").chooseAction(view(ATTACK)));
    }

    @Test
    void chooseAction_numbers_playThoseHandCards() {
        assertEquals(new TurnAction.Play(ATTACK, TACO_CAT),
                controller("1 3\n").chooseAction(view(ATTACK, SKIP, TACO_CAT)));
    }

    @Test
    void chooseAction_invalidInput_askedAgain() {
        // not a number, out of range, same card twice, then pass
        assertEquals(new TurnAction.Pass(), controller("x\n9\n1 1\nd\n").chooseAction(view(ATTACK)));
        assertEquals(3, printed().split("Invalid input", -1).length - 1);
    }

    @Test
    void chooseAction_showsHandAndOpponents() {
        controller("d\n").chooseAction(view(ATTACK, SKIP));
        assertTrue(printed().contains("1) Attack"));
        assertTrue(printed().contains("2) Skip"));
        assertTrue(printed().contains("Bob 3"));
        assertTrue(printed().contains("Deck 10"));
    }

    @Test
    void chooseAction_dashboardShowsRecentEvents() {
        ConsoleOutput out = new ConsoleOutput(new PrintStream(output, true));
        out.event("Bob played Attack.", Style.Tone.NORMAL);
        new ConsoleController(new BufferedReader(new StringReader("d\n")), out, List.of())
                .chooseAction(view(SKIP));
        assertTrue(printed().lines().anyMatch(l -> l.startsWith("│ Bob played Attack.")));
    }

    @Test
    void privateLines_areKeptForTheDashboard() {
        ConsoleOutput out = new ConsoleOutput(new PrintStream(output, true));
        ConsoleController controller = new ConsoleController(new BufferedReader(new StringReader("")), out, List.of());
        controller.handChanged(new HandChange.Drawn(new Card(SKIP)));
        controller.seeTheFuture(List.of(new Card(NOPE)));
        assertEquals(List.of("You drew Skip.", "The top of the deck: Nope (top first)"),
                out.recent().stream().map(ConsoleOutput.Line::text).toList());
    }

    @Test
    void chooseAction_pairOfGroupedCards_byRepeatingItsNumber() {
        assertEquals(new TurnAction.Play(TACO_CAT, TACO_CAT),
                controller("2 2\n").chooseAction(view(TACO_CAT, SKIP, TACO_CAT)));
        assertTrue(printed().contains("2) Taco Cat ×2"));
    }

    @Test
    void chooseAction_h_explainsTheCards_thenAsksAgain() {
        assertEquals(new TurnAction.Pass(), controller("h\nd\n").chooseAction(view(SKIP)));
        assertTrue(printed().contains("Skip: " + SKIP.description()));
    }

    @Test
    void lateNopeEnter_isNotTakenAsAnAnswerToTheNextPrompt() {
        assertEquals(new TurnAction.Pass(), controller("\nd\n").chooseAction(view(SKIP)));
        assertTrue(printed().contains("No Nope window is open"));
        assertFalse(printed().contains("Invalid input"));
    }

    @Test
    void handChanged_namesTheCard() {
        ConsoleController controller = controller("");
        controller.handChanged(new HandChange.Drawn(new Card(SKIP)));
        controller.handChanged(new HandChange.Received(new Card(NOPE), "Bob"));
        controller.handChanged(new HandChange.Lost(new Card(DEFUSE), "Bob"));
        assertTrue(printed().contains("You drew Skip."));
        assertTrue(printed().contains("You got Nope from Bob."));
        assertTrue(printed().contains("Your Defuse went to Bob."));
    }

    @Test
    void chooseKittenPosition_outOfRange_askedAgain() {
        assertEquals(2, controller("5\n-1\n2\n").chooseKittenPosition(view(), 3));
    }

    @Test
    void chooseTarget_oneBasedInput_zeroBasedIndex() {
        List<OpponentView> candidates = List.of(new OpponentView("Bob", 1, true), new OpponentView("Cid", 2, true));
        assertEquals(1, controller("2\n").chooseTarget(view(), candidates));
    }

    @Test
    void chooseCardToGive_returnsHandIndex() {
        assertEquals(0, controller("0\n1\n").chooseCardToGive(view(NOPE, SKIP), "Bob"));
        assertTrue(printed().contains("Bob asks you for a Favor"));
    }

    @Test
    void chooseCardToGive_menuNumberMapsBackToTheHand() {
        // menu sorted by name: 1) Nope, 2) Skip ×2, 3) Taco Cat
        assertEquals(2, controller("3\n").chooseCardToGive(view(SKIP, SKIP, TACO_CAT, NOPE), "Bob"));
    }

    @Test
    void nameCard_picksFromList() {
        assertEquals(NOPE, controller("2\n").nameCard(view()));
    }

    @Test
    void seeTheFuture_printsTopCards() {
        controller("").seeTheFuture(List.of(new Card(EXPLODING_KITTEN), new Card(SKIP)));
        assertTrue(printed().contains("Exploding Kitten, Skip (top first)"));
    }

    @Test
    void offerNope_enter_playsNope() {
        FakeOffer offer = new FakeOffer();
        controller("\n").offerNope(view(NOPE), offer);
        assertTrue(offer.noped);
        assertTrue(printed().contains("Nope Bob's Attack?"));
    }

    @Test
    void offerNope_plainConsole_showsTheWindowLength() {
        controller("\n").offerNope(view(NOPE), new FakeOffer());
        assertTrue(printed().contains("Nope Bob's Attack? [Enter to Nope, 5s]"));
    }

    @Test
    void offerNope_colourConsole_countsDown() {
        Instant[] now = {Instant.EPOCH};
        FakeOffer offer = new FakeOffer() {
            int checks;

            @Override
            public boolean isOpen() {
                now[0] = now[0].plusSeconds(1);       // a second passes at every check
                return ++checks <= 5;
            }
        };
        Reader nothingTyped = new Reader() {
            public int read(char[] buf, int off, int len) { throw new AssertionError("nothing to read"); }
            public boolean ready() { return false; }
            public void close() { }
        };
        new ConsoleController(new BufferedReader(nothingTyped),
                new ConsoleOutput(new PrintStream(output, true), Style.ANSI), List.of(), () -> now[0])
                .offerNope(view(NOPE), offer);
        assertTrue(printed().contains("■■■■■ 5s"));
        assertTrue(printed().contains("■■■□□ 3s"));
        assertFalse(printed().endsWith("s "), "the expired prompt is removed from the screen");
    }

    @Test
    void offerNope_windowClosed_leavesInputForNextPrompt() {
        FakeOffer offer = new FakeOffer();
        offer.open = false;
        ConsoleController controller = controller("d\n");
        controller.offerNope(view(NOPE), offer);
        assertFalse(offer.noped);
        assertEquals(new TurnAction.Pass(), controller.chooseAction(view()));
    }

    @Test
    void inputClosed_throws() {
        assertThrows(IllegalStateException.class, () -> controller("").chooseAction(view()));
    }
}
