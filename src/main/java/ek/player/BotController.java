package ek.player;

import ek.model.Card;
import ek.model.CardType;
import ek.player.GameView.OpponentView;
import ek.util.Randomizer;
import ek.util.Sleeper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A computer player that makes random legal choices (rule 16).
 *
 * <p>Thread-safe as long as the {@link Randomizer} is ({@code JavaRandomizer} is):
 * {@link #offerNope} may run on another thread.
 */
public final class BotController implements PlayerController {

    /** Shortest time the bot takes to play a Nope, so humans get a chance first. */
    static final Duration MIN_NOPE_DELAY = Duration.ofSeconds(1);

    /** The bot Nopes at least this long before the window ends, so it is never too late. */
    static final Duration NOPE_MARGIN = Duration.ofMillis(500);

    private final Randomizer randomizer;
    private final Set<CardType> playableAlone;
    private final List<CardType> nameableCards;
    private final Sleeper sleeper;

    /**
     * @param playableAlone card types that have an effect when played on their own
     * @param nameableCards card types the bot may ask for with three of a kind
     * @param sleeper       waits the bot's answer time before a Nope; tests pass {@link Sleeper#NONE}
     */
    public BotController(Randomizer randomizer, Set<CardType> playableAlone, List<CardType> nameableCards,
                         Sleeper sleeper) {
        this.randomizer = randomizer;
        this.playableAlone = Set.copyOf(playableAlone);
        this.nameableCards = List.copyOf(nameableCards);
        this.sleeper = sleeper;
    }

    /** Picks uniformly among Pass and every legal play: single cards, pairs and triples. */
    @Override
    public TurnAction chooseAction(GameView view) {
        List<TurnAction> options = new ArrayList<>();
        options.add(new TurnAction.Pass());
        for (CardType type : distinctTypes(view.hand())) {
            long count = view.hand().stream().filter(c -> c.type().equals(type)).count();
            if (playableAlone.contains(type)) {
                options.add(new TurnAction.Play(type));
            }
            if (count >= 2) {
                options.add(new TurnAction.Play(type, type));
            }
            if (count >= 3) {
                options.add(new TurnAction.Play(type, type, type));
            }
        }
        return options.get(randomizer.nextInt(options.size()));
    }

    @Override
    public int chooseKittenPosition(GameView view, int deckSize) {
        return randomizer.nextInt(deckSize + 1);
    }

    @Override
    public int chooseTarget(GameView view, List<OpponentView> candidates) {
        return randomizer.nextInt(candidates.size());
    }

    @Override
    public int chooseCardToGive(GameView view, String requester) {
        return randomizer.nextInt(view.hand().size());
    }

    @Override
    public CardType nameCard(GameView view) {
        return nameableCards.get(randomizer.nextInt(nameableCards.size()));
    }

    @Override
    public void seeTheFuture(List<Card> topCards) {
        // a random bot doesn't use what it sees
    }

    /**
     * Nopes half the time (if it holds a Nope), at a random moment inside the window: 1 s
     * after it opens at the earliest, {@link #NOPE_MARGIN} before it ends at the latest.
     * Otherwise it returns at once; the window still runs its full length, so the bot's
     * choice never changes how long the game waits.
     */
    @Override
    public void offerNope(GameView view, NopeOffer offer) {
        if (offer.canNope() && randomizer.nextInt(2) == 0) {
            sleeper.sleep(nopeDelay(offer.window()));
            offer.playNope();
        }
    }

    /** Uniform in [{@link #MIN_NOPE_DELAY}, window − {@link #NOPE_MARGIN}], to the millisecond. */
    private Duration nopeDelay(Duration window) {
        long spread = Math.max(0, window.minus(NOPE_MARGIN).minus(MIN_NOPE_DELAY).toMillis());
        return MIN_NOPE_DELAY.plusMillis(randomizer.nextInt((int) spread + 1));
    }

    private static Set<CardType> distinctTypes(List<Card> hand) {
        Set<CardType> types = new LinkedHashSet<>();
        hand.forEach(c -> types.add(c.type()));
        return types;
    }
}
