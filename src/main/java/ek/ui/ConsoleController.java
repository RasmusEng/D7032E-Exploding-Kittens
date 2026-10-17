package ek.ui;

import ek.model.Card;
import ek.model.CardType;
import ek.player.CardChoice;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.player.HandChange;
import ek.player.NopeOffer;
import ek.player.NopeWindow;
import ek.player.PlayerController;
import ek.player.TurnAction;
import ek.ui.Style.Tone;
import ek.util.Sleeper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A human player at the console. All input is checked here (the boundary) and asked again
 * until it is valid; the engine only gets well-formed answers. Each turn starts by drawing
 * the {@link Dashboard}, and every later decision updates it from its fresh {@link GameView}.
 *
 * <p><b>Thread-safety.</b> {@link #offerNope} and {@link #watchNopeWindow} run on a
 * Nope-window thread while the engine thread waits. Every read takes {@code inputLock}, and
 * the Nope countdown only reads while the window is open, so a line is never read by two
 * prompts. Output goes through the thread-safe {@link ConsoleOutput}.
 */
public final class ConsoleController implements PlayerController {

    /** How often the Nope countdown checks for input and for the window closing. */
    private static final Duration NOPE_POLL = Duration.ofMillis(50);

    private final BufferedReader in;
    private final ConsoleOutput out;
    private final List<CardType> nameableCards;
    private final Sleeper sleeper;
    private final Object inputLock = new Object();

    /**
     * @param nameableCards card types offered when naming a card for three of a kind
     * @param sleeper       paces the Nope countdown; tests pass one that doesn't really wait
     */
    public ConsoleController(BufferedReader in, ConsoleOutput out, List<CardType> nameableCards, Sleeper sleeper) {
        this.in = in;
        this.out = out;
        this.nameableCards = List.copyOf(nameableCards);
        this.sleeper = sleeper;
    }

    @Override
    public TurnAction chooseAction(GameView view) {
        HandMenu hand = new HandMenu(view.hand());
        out.showBoard(board(view, hand));
        while (true) {
            String line = ask("Play cards by number (2, or 3 3 for a pair), d = draw, h = help: ");
            if (line.equalsIgnoreCase("d") || line.equalsIgnoreCase("draw")) {
                return new TurnAction.Pass();
            }
            if (line.equalsIgnoreCase("h") || line.equalsIgnoreCase("help")) {
                printHelp(hand);
                continue;
            }
            Optional<List<CardType>> cards = hand.parsePlay(line);
            if (cards.isPresent()) {
                return new TurnAction.Play(cards.get());
            }
            out.println("Invalid input. Type h for help.");
        }
    }

    /** Shows the dashboard as soon as the cards are dealt, before anyone's turn. */
    @Override
    public void gameStarted(GameView view) {
        out.showBoard(board(view, new HandMenu(view.hand())));
    }

    @Override
    public int chooseKittenPosition(GameView view, int deckSize) {
        out.updateBoard(board(view, new HandMenu(view.hand())));
        out.println("You defused the Exploding Kitten! Put it back into the deck (" + deckSize + " cards).",
                Tone.GOOD);
        return askNumber("Position (0 = top, " + deckSize + " = bottom): ", 0, deckSize);
    }

    @Override
    public int chooseTarget(GameView view, List<OpponentView> candidates) {
        out.updateBoard(board(view, new HandMenu(view.hand())));
        printNumbered("Choose a player:", candidates.stream()
                .map(o -> o.name() + " (" + cards(o.handSize()) + ")").toList());
        return askNumber("Player number: ", 1, candidates.size()) - 1;
    }

    @Override
    public int chooseCardToGive(GameView view, String requester) {
        HandMenu hand = new HandMenu(view.hand());
        out.updateBoard(board(view, hand));
        printLines(requester + " asks you for a Favor. Your hand:", hand.lines());
        return hand.handIndexOf(askNumber("Card to give: ", 1, hand.size()));
    }

    @Override
    public CardType nameCard(GameView view) {
        out.updateBoard(board(view, new HandMenu(view.hand())));
        printNumbered("Name a card to ask for:", nameableCards.stream().map(CardType::displayName).toList());
        return nameableCards.get(askNumber("Card number: ", 1, nameableCards.size()) - 1);
    }

    @Override
    public List<Integer> chooseCards(GameView view, CardChoice choice) {
        out.updateBoard(board(view, new HandMenu(view.hand())));
        printNumbered(choice.prompt(), choice.options().stream().map(Card::toString).toList());
        String prompt = choice.picks() == 1 ? "Card number: " : "Card numbers, in order: ";
        while (true) {
            Optional<List<Integer>> picked = parseNumbers(ask(prompt))
                    .map(numbers -> numbers.stream().map(n -> n - 1).toList());
            if (picked.isPresent() && choice.accepts(picked.get())) {
                return picked.get();
            }
            out.println("Enter " + choice.picks() + " different numbers from 1 to " + choice.options().size() + ".");
        }
    }

    @Override
    public void seeTheFuture(List<Card> topCards) {
        out.event("The top of the deck: " + topCards.stream().map(Card::toString)
                .collect(Collectors.joining(", ")) + (topCards.isEmpty() ? "(empty)" : " (top first)"), Tone.GOOD);
    }

    @Override
    public void handChanged(HandChange change) {
        out.event(switch (change) {
            case HandChange.Drawn d -> "You drew " + d.card() + ".";
            case HandChange.Received r -> "You got " + r.card() + " from " + r.from() + ".";
            case HandChange.Lost l -> "Your " + l.card() + " went to " + l.to() + ".";
        }, Tone.NORMAL);
    }

    /**
     * Shows the Nope window as a countdown until it closes; only the engine (or the server)
     * closes it. With a Nope, Enter plays it. Without one, the same countdown is shown and
     * Enter only says so, so nobody can tell from the screen or the timing who holds a Nope.
     */
    @Override
    public void offerNope(GameView view, NopeOffer offer) {
        out.updateBoard(board(view, new HandMenu(view.hand())));
        String play = offer.playedBy() + "'s " + ConsoleView.describe(offer.cards());
        if (!offer.canNope()) {
            countdown(offer, "Nope window for " + play, answer -> {
                out.println("You have no Nope.");
                return false;
            });
            return;
        }
        String question = offer.nopesSoFar() % 2 == 1
                ? "Play Nope to Yup " + play + " back on? Enter = Nope"
                : "Nope " + play + "? Enter = Nope";
        countdown(offer, question, answer -> {
            if (!answer.isEmpty()) {
                out.println("Press Enter to Nope.");
                return false;
            }
            if (!offer.playNope()) {
                out.event("Too late!", Tone.MUTED);
            }
            return true;
        });
    }

    /** Shows the countdown of the window on this player's own play (or Nope). */
    @Override
    public void watchNopeWindow(GameView view, NopeWindow window) {
        out.updateBoard(board(view, new HandMenu(view.hand())));
        String text = window.nopesSoFar() == 0
                ? "Waiting for Nopes on your " + ConsoleView.describe(window.cards())
                : "Waiting for Nopes on your Nope";
        countdown(window, text, answer -> {
            out.println("You can't Nope your own play.");
            return false;
        });
    }

    /**
     * Prompts with {@code text} and the seconds left until {@code window} closes. A redrawing
     * console rewrites only the prompt's line once a second; a plain one prints the prompt
     * once with the window's length. Time is counted from the {@link Sleeper}'s pauses: only
     * for display, as the window closes when the engine (or server) says so.
     *
     * @param onAnswer handles a typed line (trimmed); returns {@code true} to stop asking
     */
    private void countdown(NopeWindow window, String text, Predicate<String> onAnswer) {
        long total = secondsLeft(window.window(), Duration.ZERO);
        long shown = total;
        long prompt = out.prompt(countdownPrompt(text, shown, total));
        Duration elapsed = Duration.ZERO;
        while (window.isOpen()) {
            Optional<String> line = readLineIfOpen(window);
            if (line.isPresent()) {
                out.answered(prompt, line.get());
                if (onAnswer.test(line.get().trim())) {
                    return;
                }
                prompt = out.prompt(countdownPrompt(text, shown, total));
                continue;
            }
            sleeper.sleep(NOPE_POLL);
            if (Thread.currentThread().isInterrupted()) {
                return;
            }
            elapsed = elapsed.plus(NOPE_POLL);
            long left = Math.max(1, secondsLeft(window.window(), elapsed));   // the window, not the clock, ends it
            if (left != shown) {
                shown = left;
                out.updatePrompt(prompt, countdownPrompt(text, shown, total));
            }
        }
        out.closePrompt(prompt, "(time's up)");
    }

    /** "text… 4" padded to the same width every second when redrawing; "text (5 s): " when plain. */
    private String countdownPrompt(String text, long secondsLeft, long totalSeconds) {
        if (!out.style().canRewriteLines()) {
            return out.style().paint(text + " (" + totalSeconds + " s): ", Tone.NOPE);
        }
        int width = String.valueOf(totalSeconds).length();
        return out.style().paint(text + "… " + String.format("%" + width + "d", secondsLeft) + " ", Tone.NOPE);
    }

    /** Whole seconds left, rounded up, never below 0. */
    static long secondsLeft(Duration window, Duration elapsed) {
        Duration left = window.minus(elapsed);
        return left.isNegative() ? 0 : (left.toMillis() + 999) / 1000;
    }

    /** The dashboard for {@code view}, numbering the hand like {@code hand}. */
    private Function<List<ConsoleOutput.Line>, List<String>> board(GameView view, HandMenu hand) {
        Dashboard dashboard = new Dashboard(out.style());
        return recent -> dashboard.render(view, hand, recent);
    }

    private void printHelp(HandMenu hand) {
        printLines("Your cards:", hand.descriptions());
        out.println("  Any 2 cards with the same name: steal a random card. Any 3: name a card to take.");
        out.println("  Pass ends your turn by drawing a card.");
    }

    private static String cards(int n) {
        return n + (n == 1 ? " card" : " cards");
    }

    private int askNumber(String prompt, int min, int max) {
        while (true) {
            Optional<Integer> n = parseInt(ask(prompt));
            if (n.isPresent() && n.get() >= min && n.get() <= max) {
                return n.get();
            }
            out.println("Enter a number from " + min + " to " + max + ".");
        }
    }

    /**
     * Prompts and reads a trimmed, non-empty line. An empty line is an Enter meant for a
     * Nope window that has closed (no prompt here takes an empty answer), so it is explained
     * and the question asked again.
     */
    private String ask(String text) {
        while (true) {
            long prompt = out.prompt(text);
            String line = readLine().trim();
            out.answered(prompt, line);
            if (!line.isEmpty()) {
                return line;
            }
            out.println("(No Nope window is open.)");
        }
    }

    private void printNumbered(String title, List<String> items) {
        out.println(title, Tone.HEADER);
        for (int i = 0; i < items.size(); i++) {
            out.println("  " + (i + 1) + ") " + items.get(i));
        }
    }

    private void printLines(String title, List<String> lines) {
        out.println(title, Tone.HEADER);
        lines.forEach(out::println);
    }

    private String readLine() {
        synchronized (inputLock) {
            try {
                return requireOpen(in.readLine());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /** Reads a line only if one is waiting and the window is still open; never blocks. */
    private Optional<String> readLineIfOpen(NopeWindow window) {
        synchronized (inputLock) {
            try {
                return window.isOpen() && in.ready() ? Optional.of(requireOpen(in.readLine())) : Optional.empty();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private static String requireOpen(String line) {
        if (line == null) {
            throw new IllegalStateException("Console input closed");
        }
        return line;
    }

    /** Numbers separated by spaces and/or commas; empty if any part is not a number. */
    static Optional<List<Integer>> parseNumbers(String s) {
        List<Integer> numbers = new ArrayList<>();
        for (String part : s.trim().split("[\\s,]+")) {
            Optional<Integer> n = parseInt(part);
            if (n.isEmpty()) {
                return Optional.empty();
            }
            numbers.add(n.get());
        }
        return Optional.of(numbers);
    }

    static Optional<Integer> parseInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
