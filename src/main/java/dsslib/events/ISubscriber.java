package dsslib.events;

import java.util.Queue;

public interface ISubscriber {

    /**
     * Publishes an event to the pubsub service
     * @param event
     */
    void subscribe(Event event, EventBus eventBus);

    /**
     * The event to to forward
     *
     * @param event
     */
    void forwardEvent(Event event);


    /**
     * The event to push
     *
     * @return
     */
    Queue<Event> getEvents();


}
