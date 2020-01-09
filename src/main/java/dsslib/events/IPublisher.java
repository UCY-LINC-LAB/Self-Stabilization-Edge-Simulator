package dsslib.events;

public interface IPublisher {

    /**
     * Publishes an event to the eventbus service
     * @param event
     */
    void publish(Event event, EventBus eventBus);

}
