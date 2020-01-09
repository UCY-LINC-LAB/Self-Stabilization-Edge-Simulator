package dsslib.scheduler;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.exceptions.*;
import dsslib.logs.DirectoryService;
import dsslib.logs.EventTracer;
import dsslib.logs.LogEntry;
import dsslib.statistics.Statistics;
import dsslib.scenarios.Scenario;
import dsslib.scenarios.ScenarioDescriptionLoader;
import dsslib.scenarios.VirtualFogTopology;
import dsslib.scenarios.environment.EnvironmentEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Scheduler implements IScheduler {

    private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

    /** For Singleton Pattern **/
    private static Scheduler instance;

    private DirectoryService directoryService;

    /** A scenario **/
    private Scenario scenario;

    /** The notion of  time: Measured in gpts units **/
    private long time=1;

    private boolean started;

    private Map<String,IProcess> processSet = new HashMap<>();

    private EventTracer eventTracer;

    private Map<String, IProcess> addressResolutionTable = new HashMap<>();

    private Set<String> enabledModulesForLog = new LinkedHashSet<>();

    private Set<String> notEnabledModulesForLog = new LinkedHashSet<>();

    /** Hold all logging **/
    private List<LogEntry> logEntries = new CopyOnWriteArrayList<>();


    private Scheduler() throws IOException {
        eventTracer = new EventTracer(DirectoryService.getInstance().getSchedulerPath(),"scheduler",true);
    }

    /**
     * Thread-safe lazy initialization
     * @return
     */
    public static synchronized Scheduler getInstance() {
        if(instance == null){
            try {
                instance = new Scheduler();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    try {
                        instance.close();
                    } catch (Exception e) {
                        logger.error("Failed to close all resources:"+e.getMessage());
                    }
                }));
                logger.trace("Lazy initialization of "+instance.getClass().getName());
            } catch (IOException e) {
                logger.info("Failed to create scheduler: "+e.getLocalizedMessage());
            }
        }
        return instance;
    }



    @Override
    public void step() throws SchedulerNotStarted, EventHandlerIsNA, MissingTopology, MissingModule {

        if(!started){
            logger.trace("Simulation has not started");
            throw new SchedulerNotStarted();
        }
        boolean logAllowed = ScenarioDescriptionLoader.getScenarioDescription().getMode().isLogsEnabled();

        //Perform an event from the scenario
        List<EnvironmentEvent> events = scenario.getNextAvailable(time);
        for( EnvironmentEvent event:events){
            scenario.handle(event);
            logScenario(event.getEventType().toString()+" for "+event.getProcessId());
        }

        boolean atLeastOneStepTaken = false;

        logger.trace("Executing step: " + time);
        for(IProcess process: processSet.values()){

            //No events are passed
            if(!process.isEnabled()) {
                //return Scheduler.getInstance().getTime()>=enableTime;
                continue;
            }

            // All enabled processes are given the ability to decide if they should execute a step
            atLeastOneStepTaken |= process.step();

            if(atLeastOneStepTaken){
                eventTracer.flushOnlyIf("#\n");
                eventTracer.setState(false);
            }

        }
        //Flushing only when there is event tracer
        if(logAllowed)
            flushTrace();

        //Compute statistics only if it is not gui
        //if(!scenario.getMode().isGui())
        Statistics.getInstance().computeAndOutput(time);

        time++;
    }

    public synchronized void flushTrace(){
        //Flush every committed event
        for(IProcess process: processSet.values()){
            if(!process.isEnabled())
                continue;
            String trace = process.getEventBus().getTrace();
            if(process.getEventTracer()==null)
                continue;
            if(!trace.isEmpty())
                process.getEventTracer().flush(trace);
            trace = process.getEventBus().getUnprocessedTrace();
            if(!trace.isEmpty())
                process.getEventTracer().flush(trace);
        }
    }
    @Override
    public void multipleSteps(int steps) throws SchedulerNotStarted, EventHandlerIsNA, MissingTopology, MissingModule {
        int progress = 100;
        try {
            progress = getScenario().getMode().getProgress_every();
        } catch (MissingScenario missingScenario) {
        }
        int count=0;
        for(int i=0;i<steps;i++){
            step();
            count+=1;
            if(count%progress==0)
                logger.info("At step: "+count);
        }
    }


    public void register(IProcess process, boolean inAddressResolutionTable){
        this.processSet.put(process.getUuid(), process);
        if(inAddressResolutionTable)
            this.addressResolutionTable.put(process.getUuid(),process);
    }
    @Override
    public void startConsole() throws MissingScenario {
        if(scenario==null)
            throw new MissingScenario();
        logger.trace("Scheduler started");
        started = true;

        Scanner scanner = new Scanner(System.in);
        System.out.printf("[%4d]> ",time);
        while(scanner.nextLine()!=null){
            try {

                this.step();
                System.out.println(Statistics.getInstance());
                //System.out.println(VirtualFogTopology.getInstance().getState());
            } catch (SchedulerNotStarted schedulerNotStarted) {
                schedulerNotStarted.printStackTrace();
                logger.info(schedulerNotStarted.getLocalizedMessage()+" shouldn't have happened");
            } catch (EventHandlerIsNA eventHandlerIsNA) {
                eventHandlerIsNA.printStackTrace();
                logger.info(eventHandlerIsNA.getLocalizedMessage());
            } catch (MissingTopology missingTopology) {
                missingTopology.printStackTrace();
                logger.info(missingTopology.getLocalizedMessage());
            } catch (MissingModule missingModule) {
                missingModule.printStackTrace();
                logger.info(missingModule.getLocalizedMessage());
            }
            System.out.printf("\n[%4d]> ",time);
        }

    }

    public long getTime() {
        return time;
    }

    @Override
    public Map<String, IProcess> getAddressResolutionTable() {
        return addressResolutionTable;
    }

    @Override
    public boolean isLogEnabled(String module) {
        if(notEnabledModulesForLog.contains(module))
            return false;

        if(enabledModulesForLog.isEmpty()) {
            return true;
        }

        return enabledModulesForLog.contains(module);
    }

    @Override
    public void registerModuleForLog(Class<? extends AbstractModule> module) {
        String name = module.getSimpleName();
        enabledModulesForLog.add(name);
    }

    @Override
    public void excludeModuleForLog(Class<? extends AbstractModule> module) {
        String name = module.getSimpleName();
        notEnabledModulesForLog.add(name);

    }

    @Override
    public Scenario getScenario() throws MissingScenario {
        if(scenario==null)
            throw new MissingScenario();
        return scenario;
    }

    @Override
    public void setScenario(Scenario scenario) {
        this.scenario = scenario;

    }

    @Override
    public Optional<IProcess> findProcess(String id) {
        return Optional.empty();
    }

    public void close() throws Exception {

        for(IProcess process: processSet.values()){
            process.close();
        }
    }

    @Override
    public VirtualFogTopology getVirtualFogTopolgy()   {
        try {
            return (VirtualFogTopology) this.scenario.getTopology();
        } catch (MissingTopology missingTopology) {
            missingTopology.printStackTrace();
            return null;
        }
    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> result = new HashMap<>();
        result.put("time",time);
        return result;
    }

    @Override
    public void pushToLogService(String moduleName, long globalTime, long localTime, String process, String module, String log) {
        logEntries.add(new LogEntry(moduleName,globalTime,localTime,process,module,log));
    }

    @Override
    public ListIterator<LogEntry> logIterator(int cursor) {
        return logEntries.listIterator(cursor);
    }

    @Override
    public EventTracer getEventTracer() {
        return eventTracer;
    }

    public void logScenario(String log){
        IScheduler scheduler = Scheduler.getInstance();
        //Normally a step up in class hierarchy should give as the module
        //if(!scheduler.isLogEnabled(this.getClass().getSuperclass().getSimpleName()))
         //   return;

        String component = ":";
        String text = String.format("%6d\t%6d\t%12s %20s %s\n", scheduler.getTime(), time,
                "Scenario",component,log);
        scheduler.getEventTracer().flush(text);
        scheduler.getEventTracer().setState(true);



    }

    public void setStarted(boolean started) {
        this.started = started;
    }

    public boolean isStarted() {
        return started;
    }

    public void setDirectoryService(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    public DirectoryService getDirectoryService() {
        return directoryService;
    }
}
