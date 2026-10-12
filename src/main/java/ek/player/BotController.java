package ek.player;

import ek.model.Card;
import ek.model.CardType;
import ek.player.GameView.OpponentView;
import ek.util.Randomizer;

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

    private final Randomizer randomizer;
    private final Set<CardType> playableAlone;
    private final List<CardType> nameableCards;

    /**
     * @param playableAlone card types that have an effect when played on their own
     * @param nameableCards card types the bot may ask for with three of a kind
     */
    public BotController(Randomizer randomizer, Set<CardType> playableAlone, List<CardType> nameableCards) {
        this.randomizer = randomizer;
        this.playableAlone = Set.copyOf(playableAlone);
        this.nameableCards = List.copyOf(nameableCards);
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

    /** Nopes half the time. */
    @Override
    public void offerNope(GameView view, NopeOffer offer) {
        if (randomizer.nextInt(2) == 0) {
            offer.playNope();
        }
    }

    private static Set<CardType> distinctTypes(List<Card> hand) {
        Set<CardType> types = new LinkedHashSet<>();
        hand.forEach(c -> types.add(c.type()));
        return types;
    }
}
