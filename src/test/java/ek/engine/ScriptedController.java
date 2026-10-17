package ek.engine;

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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Fake player with pre-programmed answers. Scripted actions are used first, then it
 * passes forever; scripted Nope answers are used first, then it never Nopes.
 * Other answers fail the test if the script runs out.
 */
public final class ScriptedController implements PlayerController {

    private final Deque<TurnAction> actions = new ArrayDeque<>();
    private final Deque<Integer> kittenPositions = new ArrayDeque<>();
    private final Deque<Integer> targets = new ArrayDeque<>();
    private final Deque<Integer> gifts = new ArrayDeque<>();
    private final Deque<CardType> namedCards = new ArrayDeque<>();
    private final Deque<List<Integer>> cardAnswers = new ArrayDeque<>();
    private final List<CardChoice> cardChoices = new ArrayList<>();
    private final List<List<Card>> futuresSeen = new ArrayList<>();
    private final List<GameView> actionViews = new ArrayList<>();
    private final List<GameView> startViews = new ArrayList<>();
    private final Deque<Boolean> nopeAnswers = new ArrayDeque<>();
    // Nope windows may arrive on other threads
    private final List<NopeOffer> nopeOffers = new CopyOnWriteArrayList<>();
    private final List<NopeWindow> watchedWindows = new CopyOnWriteArrayList<>();
    private final List<HandChange> handChanges = new ArrayList<>();
    private Integer defaultPosition;
    private int positionRequests;
    private int giftRequests;

    /** Passes every turn; puts defused kittens at the given positions, in order. */
    public static ScriptedController passing(Integer... kittenPositions) {
        ScriptedController controller = new ScriptedController();
        controller.kittenPositions.addAll(List.of(kittenPositions));
        return controller;
    }

    /** Next action: play these cards. */
    public ScriptedController play(CardType... cards) {
        actions.add(new TurnAction.Play(cards));
        return this;
    }

    /** Next action: pass. */
    public ScriptedController pass() {
        actions.add(new TurnAction.Pass());
        return this;
    }

    /** Answers to "which player?", as indexes into the candidates. */
    public ScriptedController targets(Integer... indexes) {
        targets.addAll(List.of(indexes));
        return this;
    }

    /** Answers to "which card do you give?", as indexes into own hand. */
    public ScriptedController gives(Integer... indexes) {
        gifts.addAll(List.of(indexes));
        return this;
    }

    /** Answers to "name a card". */
    public ScriptedController names(CardType... types) {
        namedCards.addAll(List.of(types));
        return this;
    }

    /** Answers to card choices (e.g. Alter the Future), as index lists into the options. */
    @SafeVarargs
    public final ScriptedController chooses(List<Integer>... answers) {
        cardAnswers.addAll(List.of(answers));
        return this;
    }

    /** Answers to Nope offers, in order: {@code true} plays Nope at once. */
    public ScriptedController nopes(Boolean... answers) {
        nopeAnswers.addAll(List.of(answers));
        return this;
    }

    /** Used once the scripted kitten positions run out, instead of failing the test. */
    public ScriptedController withDefaultPosition(int position) {
        defaultPosition = position;
        return this;
    }

    @Override
    public TurnAction chooseAction(GameView view) {
        actionViews.add(view);
        return actions.isEmpty() ? new TurnAction.Pass() : actions.poll();
    }

    @Override
    public int chooseKittenPosition(GameView view, int deckSize) {
        positionRequests++;
        if (!kittenPositions.isEmpty()) {
            return kittenPositions.poll();
        }
        if (defaultPosition == null) {
            fail("ScriptedController ran out of kitten positions");
        }
        return defaultPosition;
    }

    @Override
    public int chooseTarget(GameView view, List<OpponentView> candidates) {
        return next(targets, "targets");
    }

    @Override
    public int chooseCardToGive(GameView view, String requester) {
        giftRequests++;
        return next(gifts, "cards to give");
    }

    @Override
    public CardType nameCard(GameView view) {
        return next(namedCards, "named cards");
    }

    @Override
    public List<Integer> chooseCards(GameView view, CardChoice choice) {
        cardChoices.add(choice);
        return next(cardAnswers, "card choices");
    }

    @Override
    public void seeTheFuture(List<Card> topCards) {
        futuresSeen.add(topCards);
    }

    @Override
    public void offerNope(GameView view, NopeOffer offer) {
        nopeOffers.add(offer);
        // scripted answers are for the windows this player can Nope in
        if (offer.canNope() && Boolean.TRUE.equals(nopeAnswers.poll())) {
            offer.playNope();
        }
    }

    @Override
    public void watchNopeWindow(GameView view, NopeWindow window) {
        watchedWindows.add(window);
    }

    @Override
    public void gameStarted(GameView view) {
        startViews.add(view);
    }

    @Override
    public void handChanged(HandChange change) {
        handChanges.add(change);
    }

    private static <T> T next(Deque<T> script, String what) {
        if (script.isEmpty()) {
            fail("ScriptedController ran out of " + what);
        }
        return script.poll();
    }

    public int positionRequests() {
        return positionRequests;
    }

    public int giftRequests() {
        return giftRequests;
    }

    public List<List<Card>> futuresSeen() {
        return futuresSeen;
    }

    /** Every card choice asked, in order, including ones asked again. */
    public List<CardChoice> cardChoices() {
        return cardChoices;
    }

    /** Every private hand change this player was told about, in order. */
    public List<HandChange> handChanges() {
        return handChanges;
    }

    /** Every Nope offer received, in order. */
    /** Windows opened on this player's own plays or Nopes. */
    public List<NopeWindow> watchedWindows() {
        return watchedWindows;
    }

    public List<NopeOffer> nopeOffers() {
        return nopeOffers;
    }

    /** The view passed to each {@link #chooseAction} call, in order. */
    /** The views passed to {@code gameStarted}; one per game. */
    public List<GameView> startViews() {
        return startViews;
    }

    public List<GameView> actionViews() {
        return actionViews;
    }
}
