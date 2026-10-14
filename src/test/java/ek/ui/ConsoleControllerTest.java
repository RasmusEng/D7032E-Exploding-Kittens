package ek.ui;

import ek.model.Card;
import ek.model.CardType;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.player.HandChange;
import ek.player.NopeOffer;
import ek.player.TurnAction;
import ek.util.Sleeper;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleControllerTest {

    private final ByteArrayOutputStream output = new ByteArrayOutputStream();

    private ConsoleController controller(String input) {
        return new ConsoleController(new BufferedReader(new StringReader(input)),
                new ConsoleOutput(new PrintStream(output, true)), List.of(DEFUSE, NOPE, TACO_CAT), Sleeper.NONE);
    }

    /** A controller for a Nope countdown: typed input, then silence; time passes only through {@code sleeper}. */
    private ConsoleController countdownController(String typed, Style style, Sleeper sleeper) {
        return new ConsoleController(new BufferedReader(new TypedInput(typed)),
                new ConsoleOutput(new PrintStream(output, true), style), List.of(NOPE), sleeper);
    }

    /** Input that holds {@code typed} and then nothing: never ready again, and never read again. */
    private static final class TypedInput extends Reader {
        private final StringReader text;
        private int left;

        TypedInput(String typed) {
            text = new StringReader(typed);
            left = typed.length();
        }

        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            if (left == 0) {
                throw new AssertionError("read input that was never typed");
            }
            int n = text.read(buffer, offset, Math.min(length, left));
            left -= n;
            return n;
        }

        @Override
        public boolean ready() {
            return left > 0;
        }

        @Override
        public void close() {
        }
    }

    /** Lets time pass instantly; closes {@code window} once its whole length has been slept, as the engine would. */
    private static final class WindowClock implements Sleeper {
        private final FakeOffer window;
        final List<Duration> sleeps = new ArrayList<>();

        WindowClock(FakeOffer window) {
            this.window = window;
        }

        @Override
        public void sleep(Duration duration) {
            sleeps.add(duration);
            if (sleeps.stream().reduce(Duration.ZERO, Duration::plus).compareTo(window.window()) >= 0) {
                window.open = false;
            }
        }

        Duration total() {
            return sleeps.stream().reduce(Duration.ZERO, Duration::plus);
        }
    }

    private static int count(String text, String part) {
        return text.split(java.util.regex.Pattern.quote(part), -1).length - 1;
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
        volatile boolean open = true;
        boolean noped;
        boolean canNope = true;

        public String playedBy() { return "Bob"; }
        public List<CardType> cards() { return List.of(ATTACK); }
        public int nopesSoFar() { return 0; }
        public Duration window() { return Duration.ofSeconds(5); }
        public boolean isOpen() { return open; }
        public boolean playNope() {
            noped = canNope && open;
            return noped;
        }
        public boolean canNope() { return canNope; }
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
        new ConsoleController(new BufferedReader(new StringReader("d\n")), out, List.of(), Sleeper.NONE)
                .chooseAction(view(SKIP));
        assertTrue(printed().lines().anyMatch(l -> l.startsWith("│ Bob played Attack.")));
    }

    @Test
    void privateLines_areKeptForTheDashboard() {
        ConsoleOutput out = new ConsoleOutput(new PrintStream(output, true));
        ConsoleController controller = new ConsoleController(new BufferedReader(new StringReader("")), out, List.of(), Sleeper.NONE);
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
    void gameStarted_showsTheDashboard_withoutAskingAnything() {
        controller("").gameStarted(view(ATTACK, SKIP));
        assertTrue(printed().contains("1) Attack"));
        assertTrue(printed().contains("Bob 3"));
        assertTrue(printed().contains("Deck 10"));
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
    void offerNope_otherInput_askedAgain() {
        FakeOffer offer = new FakeOffer();
        controller("x\n\n").offerNope(view(NOPE), offer);
        assertTrue(offer.noped);
        assertTrue(printed().contains("Press Enter to Nope."));
    }

    @Test
    void offerNope_ansi_countsDownOncePerSecond_throughTheSleeper() {
        FakeOffer offer = new FakeOffer();
        WindowClock clock = new WindowClock(offer);
        countdownController("", Style.ANSI, clock).offerNope(view(NOPE), offer);

        assertEquals(Duration.ofSeconds(5), clock.total());      // no real waiting: the fake clock ran it
        String text = printed();
        assertTrue(text.contains("Nope Bob's Attack? Enter = Nope… 5"), text);
        for (int second = 4; second >= 1; second--) {
            assertTrue(text.contains("Enter = Nope… " + second), "shows " + second);
        }
        assertFalse(text.contains("Enter = Nope… 0"));
        assertFalse(offer.noped);
    }

    @Test
    void offerNope_ansi_tickRewritesOnlyThePromptLine_notTheFrame() {
        FakeOffer offer = new FakeOffer();
        countdownController("", Style.ANSI, new WindowClock(offer)).offerNope(view(NOPE), offer);

        String text = printed();
        assertEquals(4, count(text, "\0337\r"), "one line rewrite per tick: 4, 3, 2, 1");
        String ticks = text.substring(text.indexOf("\0337\r"), text.lastIndexOf("\0338"));
        assertFalse(ticks.contains("\033[H"), "no full redraw between ticks, so typing isn't wiped");
    }

    @Test
    void offerNope_plain_printsThePromptOnce_withoutTicks() {
        FakeOffer offer = new FakeOffer();
        countdownController("", Style.PLAIN, new WindowClock(offer)).offerNope(view(NOPE), offer);

        String text = printed();
        assertEquals(1, count(text, "Nope Bob's Attack? Enter = Nope (5 s): "), text);
        assertFalse(text.contains("… 4"));
        assertFalse(text.contains("\0337"));
    }

    @Test
    void offerNope_withoutANope_sameCountdown_enterSaysSo_andKeepsCounting() {
        FakeOffer offer = new FakeOffer();
        offer.canNope = false;
        WindowClock clock = new WindowClock(offer);
        countdownController("\n", Style.PLAIN, clock).offerNope(view(SKIP), offer);

        assertTrue(printed().contains("Nope window for Bob's Attack (5 s): "), printed());
        assertTrue(printed().contains("You have no Nope."));
        assertFalse(offer.noped);
        assertEquals(Duration.ofSeconds(5), clock.total());     // it kept counting to the end
    }

    @Test
    void offerNope_withNopesSoFar_asksToYup() {
        FakeOffer offer = new FakeOffer() {
            @Override
            public int nopesSoFar() { return 1; }
        };
        controller("\n").offerNope(view(NOPE), offer);
        assertTrue(printed().contains("Play Nope to Yup Bob's Attack back on?"), printed());
    }

    @Test
    void watchNopeWindow_showsTheCountdownOnYourOwnPlay() {
        FakeOffer window = new FakeOffer();
        WindowClock clock = new WindowClock(window);
        countdownController("\n", Style.PLAIN, clock).watchNopeWindow(view(SKIP), window);

        assertTrue(printed().contains("Waiting for Nopes on your Attack (5 s): "), printed());
        assertTrue(printed().contains("You can't Nope your own play."));
        assertEquals(Duration.ofSeconds(5), clock.total());
    }

    @Test
    void countdown_closedByTheServer_endsThePrompt() {
        FakeOffer offer = new FakeOffer();
        // the window closes after 2 s (e.g. NopeClosed from the server after someone else's Nope)
        Sleeper closesEarly = new Sleeper() {
            Duration slept = Duration.ZERO;
            public void sleep(Duration d) {
                slept = slept.plus(d);
                if (slept.compareTo(Duration.ofSeconds(2)) >= 0) {
                    offer.open = false;
                }
            }
        };
        countdownController("", Style.PLAIN, closesEarly).offerNope(view(NOPE), offer);
        assertTrue(printed().contains("(time's up)"));
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
