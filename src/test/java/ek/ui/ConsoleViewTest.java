package ek.ui;

import ek.engine.GameEvent;
import ek.model.Player;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static ek.model.BaseCardType.*;
import static org.junit.jupiter.api.Assertions.*;

class ConsoleViewTest {

    private static final String NL = System.lineSeparator();

    private final Player ann = new Player("Ann");
    private final Player bob = new Player("Bob");
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final List<Duration> pauses = new ArrayList<>();
    /** Ann's console. */
    private final ConsoleView view = new ConsoleView(new ConsoleOutput(new PrintStream(output, true)), "Ann", pauses::add);

    private String render(GameEvent event) {
        return view.render(event).orElseThrow();
    }

    @Test
    void onEvent_printsOneLine() {
        view.onEvent(new GameEvent.Winner(bob));
        assertEquals("Bob wins the game!" + NL, output.toString());
    }

    @Test
    void turnStarted_hasABlankLineBefore() {
        view.onEvent(new GameEvent.TurnStarted(bob, 1));
        assertEquals(NL + "=== Bob's turn ===" + NL, output.toString());
    }

    @Test
    void render_turnWithSeveralTurnsToTake() {
        assertEquals("=== Bob's turn (2 turns to take) ===", render(new GameEvent.TurnStarted(bob, 2)));
        assertEquals("=== Your turn ===", render(new GameEvent.TurnStarted(ann, 1)));
    }

    @Test
    void render_viewerIsCalledYou() {
        assertEquals("You played Attack.", render(new GameEvent.CardsPlayed(ann, List.of(ATTACK))));
        assertEquals("Your Attack was Noped.", render(new GameEvent.PlayCancelled(ann, List.of(ATTACK))));
        assertEquals("BOOM! You exploded and are out.", render(new GameEvent.PlayerExploded(ann)));
        assertEquals("BOOM! Bob exploded and is out.", render(new GameEvent.PlayerExploded(bob)));
    }

    @Test
    void render_expansionMechanics() {
        assertEquals("Bob reversed the direction of play.", render(new GameEvent.DirectionReversed(bob)));
        assertEquals("You reversed the direction of play.", render(new GameEvent.DirectionReversed(ann)));
        assertEquals("Bob put the Exploding Kitten back into the deck face up.",
                render(new GameEvent.CardPutBackFaceUp(bob, EXPLODING_KITTEN)));
        assertEquals("You put the Exploding Kitten back into the deck face up.",
                render(new GameEvent.CardPutBackFaceUp(ann, EXPLODING_KITTEN)));
        assertEquals("Bob is out of the game (Taco Cat).", render(new GameEvent.PlayerEliminated(bob, TACO_CAT)));
        assertEquals("You are out of the game (Taco Cat).", render(new GameEvent.PlayerEliminated(ann, TACO_CAT)));
    }

    @Test
    void tone_andPause_forExpansionMechanics() {
        assertEquals(Style.Tone.DANGER, ConsoleView.tone(new GameEvent.PlayerEliminated(bob, TACO_CAT)));
        assertEquals(Style.Tone.DANGER, ConsoleView.tone(new GameEvent.CardPutBackFaceUp(bob, TACO_CAT)));
        assertEquals(Style.Tone.NORMAL, ConsoleView.tone(new GameEvent.DirectionReversed(bob)));
        view.onEvent(new GameEvent.DirectionReversed(bob));
        view.onEvent(new GameEvent.DirectionReversed(ann));
        assertEquals(1, pauses.size(), "only other players' moves pause");
    }

    @Test
    void render_combo() {
        assertEquals("Bob played Taco Cat ×2.",
                render(new GameEvent.CardsPlayed(bob, List.of(TACO_CAT, TACO_CAT))));
    }

    @Test
    void render_nopeAndYup() {
        assertEquals("Bob played Nope!", render(new GameEvent.NopePlayed(bob, true)));
        assertEquals("You played Nope (Yup)!", render(new GameEvent.NopePlayed(ann, false)));
        assertEquals("Bob's Attack was Noped.", render(new GameEvent.PlayCancelled(bob, List.of(ATTACK))));
    }

    @Test
    void render_othersPrivateCardsNotRevealed() {
        Player cid = new Player("Cid");
        assertEquals("Bob drew a card.", render(new GameEvent.CardDrawn(bob)));
        assertEquals("Cid took a card from Bob.", render(new GameEvent.CardTaken(bob, cid)));
    }

    @Test
    void render_viewersOwnCardMoves_leftToTheController() {
        assertEquals(Optional.empty(), view.render(new GameEvent.CardDrawn(ann)));
        assertEquals(Optional.empty(), view.render(new GameEvent.CardTaken(ann, bob)));
        assertEquals(Optional.empty(), view.render(new GameEvent.CardTaken(bob, ann)));
    }

    @Test
    void render_onlyTheViewerSeesTheirRejectedMoves() {
        assertEquals("Not allowed: no", render(new GameEvent.MoveRejected(ann, "no")));
        assertEquals(Optional.empty(), view.render(new GameEvent.MoveRejected(bob, "no")));
    }

    @Test
    void pausesAfterOtherPlayersMoves_only() {
        view.onEvent(new GameEvent.TurnStarted(bob, 1));
        view.onEvent(new GameEvent.CardsPlayed(bob, List.of(SKIP)));
        view.onEvent(new GameEvent.CardsPlayed(ann, List.of(SKIP)));
        view.onEvent(new GameEvent.CardDrawn(ann));
        assertEquals(List.of(ConsoleView.PAUSE), pauses);
    }

    @Test
    void rule13_winnerAnnounced() {
        assertEquals("Bob wins the game!", render(new GameEvent.Winner(bob)));
        assertEquals("You win the game!", render(new GameEvent.Winner(ann)));
    }

    @Test
    void tone_explosionsAreDanger_winsAreGood_nopesAreNope() {
        assertEquals(Style.Tone.DANGER, ConsoleView.tone(new GameEvent.PlayerExploded(bob)));
        assertEquals(Style.Tone.GOOD, ConsoleView.tone(new GameEvent.Winner(bob)));
        assertEquals(Style.Tone.NOPE, ConsoleView.tone(new GameEvent.NopePlayed(bob, true)));
        assertEquals(Style.Tone.NORMAL, ConsoleView.tone(new GameEvent.CardsPlayed(bob, List.of(SKIP))));
    }

    @Test
    void eventsAreKeptForTheDashboard_turnHeadersAreNot() {
        ConsoleOutput out = new ConsoleOutput(new PrintStream(output, true));
        ConsoleView annsView = new ConsoleView(out, "Ann", pauses::add);
        annsView.onEvent(new GameEvent.TurnStarted(bob, 1));
        annsView.onEvent(new GameEvent.CardsPlayed(bob, List.of(SKIP)));
        assertEquals(List.of(new ConsoleOutput.Line("Bob played Skip.", Style.Tone.NORMAL)), out.recent());
    }
}
