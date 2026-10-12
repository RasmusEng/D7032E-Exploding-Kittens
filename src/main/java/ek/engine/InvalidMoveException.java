package ek.engine;

/** A player tried a move the rules don't allow. The message says why, for the player. */
public final class InvalidMoveException extends Exception {

    private static final long serialVersionUID = 1L;

    public InvalidMoveException(String message) {
        super(message);
    }
}
