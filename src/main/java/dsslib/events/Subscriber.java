package dsslib.events;

import java.util.LinkedList;
import java.util.Queue;

public class Subscriber implements ISubscriber {

    private final Queue<Event> eventQueue;

    public Subscriber() {
        this.eventQueue = new LinkedList<>();

    }

    @Override
    public void subscribe(Event event, EventBus eventBus) {
        eventBus.subscribe(this,event.getClass());
    }

    @Override
    public void forwardEvent(Event event) {
        eventQueue.add(event);
    }

    @Override
    public Queue<Event> getEvents() {
        return eventQueue;
    }

    @Override
    public String toString() {
        return "Subscriber{" +
                "eventQueue=" + eventQueue +
                '}';
    }
}
