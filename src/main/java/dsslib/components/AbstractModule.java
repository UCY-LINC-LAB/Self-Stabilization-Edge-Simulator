package dsslib.components;

import dsslib.process.IProcess;
import dsslib.scheduler.IScheduler;
import dsslib.scheduler.Scheduler;
import dsslib.events.*;
import dsslib.exceptions.EventHandlerIsNA;
import dsslib.exceptions.MissingModule;
import dsslib.exceptions.MissingScenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public abstract class AbstractModule implements IModule {
    private static final Logger logger = LoggerFactory.getLogger(AbstractModule.class);

    /** I am a subscriber **/
    private ISubscriber subscriber;

    /** Also a producer **/
    private IPublisher publisher;

    /** The component belongs to a particular process **/
    private final IProcess process;

    /** Store all handlers of the waitingEvents for the component **/
    private Map<String, Set<EventHandler<? extends Event>>> eventHandlerMaps = new HashMap<>();

    /** A set of required modules **/
    private Set<String> requiredModules = new LinkedHashSet<>();

    public abstract EventHandler<Init> onInit();
    public abstract  EventHandler<Execution> onExecution();
    private final Map<String,Object> props;

    public AbstractModule(IProcess process, Map<String,Object> props) throws MissingModule{
        this.process = process;
        this.props = props;
        subscriber = new Subscriber();
        publisher = new Publisher();

        subscribe(onInit(),Init.class);
        subscribe(onExecution(),Execution.class);

        for(String module : requiredModules){
            //TODO

        }

    }

    public AbstractModule trigger(Event event){

        if(process.isEnabled()) {
            logger.trace(this.process.getUuid() + ": " + this.getClass().getSimpleName() + " triggered " + event.getClass().getTypeName());
            publisher.publish(event, process.getEventBus());
        }
        return this;
    }

    @Override
    public void require(Class<? extends IModule> module) throws MissingModule {
       String type = module.getTypeName();
       requiredModules.add(type);
       if(!process.containsModule(module)){
          throw new MissingModule(module);
       }else{
           //TODO subscirbe the process to events
       }
    }

    public void subscribe(EventHandler<? extends Event> handler, Class<? extends Event> event){
        subscribe(handler,event,process.getEventBus());
    }

    public void subscribe(EventHandler<? extends Event> handler, Class<? extends Event> event, EventBus bus){
        bus.subscribe(subscriber,event);
        String evType = event.getTypeName();
        Set<EventHandler<?>> handlers = eventHandlerMaps.getOrDefault(evType, new LinkedHashSet<>());
        handlers.add(handler);
        eventHandlerMaps.putIfAbsent(evType,handlers);
        logger.trace(this.getClass().getTypeName()+" of '"+ this.getProcess().getUuid()+"' subscribed to event "+evType);

    }

    public void bindOn(EventHandler<? extends Event> event, Class<? extends Event> type, EventBus from, EventBus to)  {
        subscribe(event,type);
        //from.forwardTo(to,type);
    }
    public void step() throws EventHandlerIsNA {
        Queue<Event> eventQueue = subscriber.getEvents();
        logger.trace("Remaining waitingEvents: "+eventQueue.size());
        while(!eventQueue.isEmpty()){

            Event event = eventQueue.poll();
            String evType = event.getClass().getTypeName();

            Set<EventHandler<?>> handlers = eventHandlerMaps.get(evType);
            if(handlers!=null){
                for (EventHandler handler : handlers) {
                    if (handler == null) {
                        throw new EventHandlerIsNA(this.getClass().getTypeName());
                    }
                    event.setGlobalTimeProcessed(Scheduler.getInstance().getTime());
                    event.setLocalTimeProcessed(this.process.getLocalTime());
                    handler.handle(event);
                }
            }
        }
    }

    @Override
    public void logToScheduler(String log){
        try {
            //TODO LOG FIX: On fast mode, do not log...
            if(!Scheduler.getInstance().getScenario().getMode().isLogsEnabled())
                return;
        } catch (MissingScenario missingScenario) {
            missingScenario.printStackTrace();
        }

        IScheduler scheduler = Scheduler.getInstance();

        String component = "("+this.getClass().getSimpleName().replaceAll("Component","")+"):";
        long globalTime = scheduler.getTime();
        long localTime = getProcess().getLocalTime();
        String process = getProcess().getUuid();
        String module = this.getClass().getSuperclass().getSimpleName().replaceAll("Module","");


        //TODO  disseminate the log t
        scheduler.pushToLogService(this.getClass().getSuperclass().getName(),globalTime,localTime,process,module,log);

        //Normally a step up in class hierarchy should give as the module
        if(!scheduler.isLogEnabled(this.getClass().getSuperclass().getSimpleName()))
            return;

        String text = String.format("%6d\t%6d\t%12s %20s %s\n", scheduler.getTime(), getProcess().getLocalTime(),
                getProcess().getUuid(),component,log);

        scheduler.getEventTracer().flush(text);
        scheduler.getEventTracer().setState(true);


    }
    public IProcess getProcess() {
        return process;
    }

}
