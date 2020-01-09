package dsslib.events;

public interface EventHandler<T extends Event> {

    void handle(T params);

}
