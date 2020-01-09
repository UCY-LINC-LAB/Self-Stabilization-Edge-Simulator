package dsslib.scheduler;

import dsslib.components.AbstractModule;
import dsslib.exceptions.*;
import dsslib.logs.EventTracer;
import dsslib.logs.LogEntry;
import dsslib.process.IProcess;
import dsslib.scenarios.Scenario;
import dsslib.scenarios.VirtualFogTopology;

import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;

public interface IScheduler {

    /**
     * A computation eventsStep of the scheduler
     */
    void step() throws SchedulerNotStarted, EventHandlerIsNA, MissingTopology, MissingModule;


    /**
     * Registers a process for scheduling
     * @param process
     * @param inAddressResolutionTable
     */
    void register(IProcess process, boolean inAddressResolutionTable);


    void multipleSteps(int steps) throws SchedulerNotStarted, EventHandlerIsNA, MissingTopology, MissingModule;
    /**
     * Start scheduler
     */
    void startConsole() throws MissingScenario;

    /**
     *
     * @return The current global time
     */
    long getTime();

    /**
     *
     * @return EventTracer
     */
    EventTracer getEventTracer();

    /**
     *  Return the process mapped to the uuid
     * @return
     */
     Map<String, IProcess> getAddressResolutionTable();


    /**
     * Check if the module has log enabled
     * @param simpleName
     * @return
     */
    boolean isLogEnabled(String simpleName);

    /**
     * Register a module for log purposes
     * @param module
     */
    void registerModuleForLog(Class<? extends AbstractModule> module);

    /**
     * Add a module to the excluded list
     * @param simpleTimerModuleClass
     */
    void excludeModuleForLog(Class<? extends AbstractModule> simpleTimerModuleClass);


    /**
     * Get the scenario
     * @return
     */
    Scenario getScenario() throws MissingScenario;

    void setScenario(Scenario scenario);

    /**
     * Return the process with uuid=id
     * @param id
     * @return
     */
    Optional<IProcess> findProcess(String id);

    VirtualFogTopology getVirtualFogTopolgy() throws MissingTopology;

    Map<String,Object> getState();

    /**
     * Push logs of the corresponding module
     * @param moduleName
     * @param globalTime
     * @param localTime
     * @param process
     * @param module
     * @param log
     */
    void pushToLogService(String moduleName, long globalTime, long localTime, String process, String module, String log);

    /**
     * @param cursor
     * @return
     */
    ListIterator<LogEntry> logIterator(int cursor);
}
