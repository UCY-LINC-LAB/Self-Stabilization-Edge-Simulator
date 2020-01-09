package dsslib.exceptions;

public class EventHandlerIsNA extends Exception{

    public EventHandlerIsNA(String module) {
        super("EventHandler is not available for module: "+module);
    }

}
