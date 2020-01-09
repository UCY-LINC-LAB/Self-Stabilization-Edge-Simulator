package dsslib.process;


import dsslib.components.IModule;
import dsslib.components.networking.NetworkModule;
import dsslib.events.Event;
import dsslib.events.EventBus;
import dsslib.events.EventHandler;
import dsslib.exceptions.EventHandlerIsNA;
import dsslib.exceptions.MissingModule;
import dsslib.exceptions.NetworkModuleNotFound;
import dsslib.logs.EventTracer;

import java.util.Map;

public interface IProcess extends AutoCloseable {
    /**
     * Return a unique ID in the scope of a simulation
     * @return
     */
    String getUuid();

    long getLocalTime();


    /**
     * Register a module
     * @param module
     */
    void registerComponent(IModule module);

    /**
     *
     * Return the process event bus
     * @return
     */
    EventBus getEventBus();

    /**
     *
     * @return
     */
    boolean step() throws EventHandlerIsNA;

    /**
     * Should the process take a local step?
     * @return
     */
    boolean isReadyForStep();

    /**
     * Is the process enabled?
     * @return
     */
    boolean isEnabled();

    /**
     * Enable or disable process
     * @param enable
     */
    void setEnabled(boolean enable);

    void trigger(Event event);

    EventTracer getEventTracer();

    /**
     * Return the available modules
     * @return
     */
    Map<String, IModule> getModules();

    /**
     * Return network module
     * @return
     */
    NetworkModule getNetworkModule() throws NetworkModuleNotFound;

    /**
     *
     * @param module
     * @return
     */
    boolean containsModule(Class<? extends IModule> module);

    IModule getModule(Class<? extends IModule> module) throws MissingModule;

    void on(Class<? extends Event> evnt, EventHandler<? extends Event> handler);

    void setZone(String zone);

    String getZone();
}
