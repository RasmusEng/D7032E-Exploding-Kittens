package ek.ui;

import ek.model.Card;
import ek.model.CardType;
import ek.player.GameView;
import ek.player.GameView.OpponentView;
import ek.player.HandChange;
import ek.player.NopeOffer;
import ek.player.PlayerController;
import ek.player.TurnAction;
import ek.ui.Style.Tone;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A human player at the console. All input is checked here (the boundary) and asked again
 * until it is valid; the engine only gets well-formed answers. Each turn starts by drawing
 * the {@link Dashboard}, and every later decision updates it from its fresh {@link GameView}.
 *
 * <p><b>Thread-safety.</b> {@link #offerNope} runs on a Nope-window thread while the engine
 * thread waits. Every read takes {@code inputLock}, and the Nope prompt only reads while the
 * window is open, so a line is never read by two prompts. Output goes through the
 * thread-safe {@link ConsoleOutput}.
 */
public final class ConsoleController implements PlayerController {

    private static final long NOPE_POLL_MILLIS = 50;

    private final BufferedReader in;
    private final ConsoleOutput out;
    private final List<CardType> nameableCards;
    private final Object inputLock = new Object();

    /** @param nameableCards card types offered when naming a card for three of a kind */
    public ConsoleController(BufferedReader in, ConsoleOutput out, List<CardType> nameableCards) {
        this.in = in;
        this.out = out;
        this.nameableCards = List.copyOf(nameableCards);
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
     * Asks whether to Nope: Enter plays it, {@code n} lets the play go ahead (so the game goes
     * on at once). Unanswered, the offer closes when the window's time is up.
     */
    @Override
    public void offerNope(GameView view, NopeOffer offer) {
        out.updateBoard(board(view, new HandMenu(view.hand())));
        String play = offer.playedBy() + "'s " + ConsoleView.describe(offer.cards());
        String question = (offer.nopesSoFar() % 2 == 1 ? "Play Nope to Yup " + play + " back on?" : "Nope " + play + "?")
                + " Enter = Nope: ";
        long prompt = out.prompt(out.style().paint(question, Tone.NOPE));
        while (offer.isOpen()) {
            Optional<String> line = readLineIfOpen(offer);
            if (line.isPresent()) {
                out.answered(prompt, line.get());
                String answer = line.get().trim();
                if (answer.isEmpty()) {
                    if (!offer.playNope()) {
                        out.event("Too late!", Tone.MUTED);
                    }
                    return;
                }
                out.println("Press Enter to Nope, or let it go.");
                prompt = out.prompt(out.style().paint(question, Tone.NOPE));
            } else if (!pause()) {
                return;
            }
        }
        out.closePrompt(prompt, "(time's up)");
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
    private Optional<String> readLineIfOpen(NopeOffer offer) {
        synchronized (inputLock) {
            try {
                return offer.isOpen() && in.ready() ? Optional.of(requireOpen(in.readLine())) : Optional.empty();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    /** @return false if interrupted (flag restored) */
    private static boolean pause() {
        try {
            Thread.sleep(NOPE_POLL_MILLIS);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static String requireOpen(String line) {
        if (line == null) {
            throw new IllegalStateException("Console input closed");
        }
        return line;
    }

    static Optional<Integer> parseInt(String s) {
        try {
            return Optional.of(Integer.parseInt(s));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
