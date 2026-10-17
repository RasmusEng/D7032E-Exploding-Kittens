package ek.engine;

import ek.model.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TurnStateTest {

    private final List<Player> players = List.of(new Player("A"), new Player("B"), new Player("C"));

    @Test
    void startAt_givesOneTurn() {
        TurnState turns = new TurnState();
        turns.startAt(2);
        assertEquals(2, turns.currentIndex());
        assertEquals(1, turns.turnsLeft());
    }

    @Test
    void endTurn_movesClockwise() {
        TurnState turns = new TurnState();
        turns.startAt(0);
        turns.endTurn(players);
        assertEquals(1, turns.currentIndex());
    }

    @Test
    void endTurn_wrapsAround() {
        TurnState turns = new TurnState();
        turns.startAt(2);
        turns.endTurn(players);
        assertEquals(0, turns.currentIndex());
    }

    @Test
    void advance_skipsExplodedPlayers() {
        players.get(1).explode();
        TurnState turns = new TurnState();
        turns.startAt(0);
        turns.advance(players);
        assertEquals(2, turns.currentIndex());
        assertEquals(1, turns.turnsLeft());
    }

    @Test
    void attack_nextPlayerGetsTwoTurns() {
        TurnState turns = new TurnState();
        turns.startAt(0);
        turns.attack(players);
        assertEquals(1, turns.currentIndex());
        assertEquals(2, turns.turnsLeft());
    }

    @Test
    void attack_onVictimsLastTurn_passesRemainingPlusTwo() {
        TurnState turns = new TurnState();
        turns.startAt(0);
        turns.attack(players);          // B: 2
        turns.endTurn(players);         // B: 1 left
        turns.attack(players);          // C: 1 + 2
        assertEquals(2, turns.currentIndex());
        assertEquals(3, turns.turnsLeft());
    }

    @Test
    void attack_afterNormalTurn_doesNotStack() {
        TurnState turns = new TurnState();
        turns.startAt(0);
        turns.attack(players);          // B: 2
        turns.endTurn(players);
        turns.endTurn(players);         // C: normal turn
        turns.attack(players);          // A: 2, not more
        assertEquals(2, turns.turnsLeft());
    }

    @Test
    void clockwise_byDefault() {
        TurnState turns = new TurnState();
        turns.startAt(0);
        assertTrue(turns.isClockwise());
    }

    @Test
    void reverse_endTurn_movesCounterClockwise() {
        TurnState turns = new TurnState();
        turns.startAt(0);
        turns.reverse();
        assertFalse(turns.isClockwise());
        turns.endTurn(players);
        assertEquals(2, turns.currentIndex());
        turns.endTurn(players);
        assertEquals(1, turns.currentIndex());
    }

    @Test
    void reverse_twice_clockwiseAgain() {
        TurnState turns = new TurnState();
        turns.startAt(0);
        turns.reverse();
        turns.reverse();
        turns.endTurn(players);
        assertEquals(1, turns.currentIndex());
    }

    @Test
    void reverse_skipsExplodedPlayersCounterClockwise() {
        players.get(2).explode();
        TurnState turns = new TurnState();
        turns.startAt(0);
        turns.reverse();
        turns.advance(players);
        assertEquals(1, turns.currentIndex());
    }

    @Test
    void reverse_twoPlayers_actsLikeSkip() {
        List<Player> two = List.of(new Player("A"), new Player("B"));
        TurnState skipped = new TurnState();
        skipped.startAt(0);
        skipped.endTurn(two);
        TurnState reversed = new TurnState();
        reversed.startAt(0);
        reversed.reverse();
        reversed.endTurn(two);
        assertEquals(skipped.currentIndex(), reversed.currentIndex());
        assertEquals(skipped.turnsLeft(), reversed.turnsLeft());
    }

    @Test
    void attack_followsTheDirection() {
        TurnState turns = new TurnState();
        turns.startAt(0);
        turns.reverse();
        turns.attack(players);
        assertEquals(2, turns.currentIndex());
        assertEquals(2, turns.turnsLeft());
    }

    @Test
    void startAt_resetsToClockwise() {
        TurnState turns = new TurnState();
        turns.reverse();
        turns.startAt(0);
        assertTrue(turns.isClockwise());
    }

    @Test
    void turnId_changesWhenTurnEnds() {
        TurnState turns = new TurnState();
        int before = turns.turnId();
        turns.endTurn(players);
        assertNotEquals(before, turns.turnId());
    }
}
