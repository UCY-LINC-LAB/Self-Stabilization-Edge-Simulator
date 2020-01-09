package dsslib.components;

import dsslib.process.IProcess;
import dsslib.exceptions.EventHandlerIsNA;
import dsslib.exceptions.MissingModule;

import java.util.Map;

/**
 * Every process hosts a set of software components, called modules in our context.
 * Each component is identified by a name, and characterized by a set of properties.
 * The component provides an interface in the form of the waitingEvents that the component
 * accepts and produces in return. Distributed programming abstractions are
 * typically made of a collection of components, at least one for every process,
 * that are intended to satisfy some common properties.
 */
public interface IModule {

    /**
     * Local computation step
     */
    void step() throws EventHandlerIsNA;

    /**
     * Retrieve the process
     * @return
     */
    IProcess getProcess();

    /**
     *
     * @param log
     */
    void logToScheduler(String log);


    /**
     * Required module
     * @param module
     */
    void require(Class<? extends IModule> module) throws MissingModule;

    /**
     * Helpful method for retrieving the state of the module
     * @return
     */
    Map<String,Object> getState();
}
