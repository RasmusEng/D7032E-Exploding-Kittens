package ek.engine;

/**
 * Receives game events (Observer). Views and the network layer implement this,
 * so the engine never does I/O itself.
 */
@FunctionalInterface
public interface GameEventListener {

    void onEvent(GameEvent event);
}
