package dsslib.events;

public class Publisher implements IPublisher {
    @Override
    public void publish(Event event, EventBus eventBus) {
        eventBus.trigger(event);
    }
}
