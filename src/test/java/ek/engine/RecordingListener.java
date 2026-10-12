package ek.engine;

import java.util.ArrayList;
import java.util.List;

/** Fake listener that keeps every event it receives, in order. */
public final class RecordingListener implements GameEventListener {

    private final List<GameEvent> events = new ArrayList<>();

    @Override
    public void onEvent(GameEvent event) {
        events.add(event);
    }

    public List<GameEvent> events() {
        return events;
    }

    /** Events of one type, in order. */
    public <T extends GameEvent> List<T> eventsOf(Class<T> type) {
        return events.stream().filter(type::isInstance).map(type::cast).toList();
    }
}
