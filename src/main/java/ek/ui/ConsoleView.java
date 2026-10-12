package ek.ui;

import ek.engine.GameEvent;
import ek.engine.GameEventListener;
import ek.model.CardType;
import ek.ui.Style.Tone;
import ek.util.Sleeper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Prints game events as text for one player, calling them "you". Only rendering: it never
 * changes the game.
 *
 * <p>Events where the viewer drew or swapped a card are not printed: the viewer's
 * {@link ConsoleController} prints those with the card's name instead. After other players'
 * moves it pauses briefly, so a run of bot moves can be read as it happens.
 */
public final class ConsoleView implements GameEventListener {

    /** How long other players' moves stay on screen before the game goes on. */
    public static final Duration PAUSE = Duration.ofMillis(800);

    private final ConsoleOutput out;
    private final String viewer;
    private final Sleeper sleeper;

    /**
     * @param viewer  the name of the player at this console
     * @param sleeper waits {@link #PAUSE} after other players' moves; {@link Sleeper#NONE} never waits
     */
    public ConsoleView(ConsoleOutput out, String viewer, Sleeper sleeper) {
        this.out = out;
        this.viewer = viewer;
        this.sleeper = sleeper;
    }

    @Override
    public void onEvent(GameEvent event) {
        Optional<String> text = render(event);
        if (text.isEmpty()) {
            return;
        }
        if (event instanceof GameEvent.TurnStarted) {
            out.heading(text.get());                  // a separator, not worth showing in "Recent"
        } else {
            out.event(text.get(), tone(event));
        }
        if (isOtherPlayersMove(event)) {
            sleeper.sleep(PAUSE);
        }
    }

    /** The text for one event, or empty if this viewer isn't shown it. Exhaustive over {@link GameEvent}. */
    Optional<String> render(GameEvent event) {
        return Optional.ofNullable(switch (event) {
            case GameEvent.TurnStarted e -> "=== " + possessive(e.player()) + " turn"
                    + (e.turnsLeft() > 1 ? " (" + e.turnsLeft() + " turns to take)" : "") + " ===";
            case GameEvent.CardDrawn e -> isViewer(e.player()) ? null : e.player() + " drew a card.";
            case GameEvent.CardsPlayed e -> name(e.player()) + " played " + describe(e.cards()) + ".";
            case GameEvent.NopePlayed e -> name(e.player()) + " played Nope" + (e.cancels() ? "!" : " (Yup)!");
            case GameEvent.PlayCancelled e -> possessive(e.player()) + " " + describe(e.cards()) + " was Noped.";
            case GameEvent.MoveRejected e -> isViewer(e.player()) ? "Not allowed: " + e.reason() : null;
            case GameEvent.CardTaken e -> isViewer(e.from()) || isViewer(e.to()) ? null
                    : e.to() + " took a card from " + e.from() + ".";
            case GameEvent.KittenDefused e -> name(e.player()) + " drew an Exploding Kitten and defused it!";
            case GameEvent.PlayerExploded e -> "BOOM! " + (isViewer(e.player())
                    ? "You exploded and are out." : e.player() + " exploded and is out.");
            case GameEvent.Winner e -> isViewer(e.player()) ? "You win the game!" : e.player() + " wins the game!";
        });
    }

    /** How an event's line is coloured. Exhaustive over {@link GameEvent}. */
    static Tone tone(GameEvent event) {
        return switch (event) {
            case GameEvent.TurnStarted e -> Tone.HEADER;
            case GameEvent.NopePlayed e -> Tone.NOPE;
            case GameEvent.PlayCancelled e -> Tone.NOPE;
            case GameEvent.MoveRejected e -> Tone.DANGER;
            case GameEvent.PlayerExploded e -> Tone.DANGER;
            case GameEvent.KittenDefused e -> Tone.GOOD;
            case GameEvent.Winner e -> Tone.GOOD;
            case GameEvent.CardDrawn e -> Tone.MUTED;
            case GameEvent.CardsPlayed e -> Tone.NORMAL;
            case GameEvent.CardTaken e -> Tone.NORMAL;
        };
    }

    /** "Attack", or "Taco Cat ×2" for combos. */
    static String describe(List<CardType> cards) {
        Map<String, Long> counts = cards.stream().collect(Collectors.groupingBy(
                CardType::displayName, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> e.getValue() > 1 ? e.getKey() + " ×" + e.getValue() : e.getKey())
                .collect(Collectors.joining(", "));
    }

    /** Moves made by someone else; turn headers and the winner aren't moves. */
    private boolean isOtherPlayersMove(GameEvent event) {
        return switch (event) {
            case GameEvent.CardDrawn e -> !isViewer(e.player());
            case GameEvent.CardsPlayed e -> !isViewer(e.player());
            case GameEvent.NopePlayed e -> !isViewer(e.player());
            case GameEvent.CardTaken e -> !isViewer(e.to());
            case GameEvent.KittenDefused e -> !isViewer(e.player());
            case GameEvent.PlayerExploded e -> !isViewer(e.player());
            default -> false;
        };
    }

    private boolean isViewer(String player) {
        return player.equals(viewer);
    }

    private String name(String player) {
        return isViewer(player) ? "You" : player;
    }

    private String possessive(String player) {
        return isViewer(player) ? "Your" : player + "'s";
    }
}
