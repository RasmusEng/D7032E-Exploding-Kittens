package ek.rules;

import ek.model.Player;

import java.util.List;

/**
 * Whose turn it is and how many turns they have left. Play goes clockwise in seat order.
 *
 * <p>{@link #turnId()} changes every time a turn ends, so callers can tell whether a
 * card effect ended the current turn.
 */
public final class TurnState {

    private static final int ATTACK_TURNS = 2;

    private int currentIndex;
    private int turnsLeft = 1;
    private boolean underAttack;
    private int turnId;

    /** Starts the game with the player at {@code index}, who has 1 turn. */
    public void startAt(int index) {
        currentIndex = index;
        turnsLeft = 1;
        underAttack = false;
    }

    /** Ends one turn (Pass or Skip); when none are left, play moves to the next alive player. */
    public void endTurn(List<Player> players) {
        turnId++;
        turnsLeft--;
        if (turnsLeft <= 0) {
            moveToNextAlive(players, 1);
        }
    }

    /**
     * Moves play to the next alive player clockwise, who gets 1 turn.
     * Any turns the current player had left are dropped (e.g. when they exploded).
     */
    public void advance(List<Player> players) {
        turnId++;
        moveToNextAlive(players, 1);
    }

    /**
     * Rule 11a: ends all the current player's turns; the next player takes 2 turns.
     * If the current player was themselves attacked, their remaining turns are added
     * (2 → 4 → 6 when each victim attacks on their first turn).
     */
    public void attack(List<Player> players) {
        int carried = underAttack ? turnsLeft : 0;
        turnId++;
        moveToNextAlive(players, carried + ATTACK_TURNS);
        underAttack = true;
    }

    private void moveToNextAlive(List<Player> players, int turns) {
        for (int step = 1; step <= players.size(); step++) {
            int candidate = (currentIndex + step) % players.size();
            if (players.get(candidate).isAlive()) {
                currentIndex = candidate;
                break;
            }
        }
        turnsLeft = turns;
        underAttack = false;
    }

    public int currentIndex() {
        return currentIndex;
    }

    public int turnsLeft() {
        return turnsLeft;
    }

    public int turnId() {
        return turnId;
    }
}
