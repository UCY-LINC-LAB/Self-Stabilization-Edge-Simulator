package dsslib.process;

import dsslib.components.AbstractModule;
import dsslib.components.DebugComponent;
import dsslib.components.DebugModule;
import dsslib.components.IModule;
import dsslib.components.networking.NetworkModule;
import dsslib.scheduler.Scheduler;
import dsslib.events.Publisher;
import dsslib.exceptions.EventHandlerIsNA;
import dsslib.exceptions.MissingModule;
import dsslib.exceptions.MissingScenario;
import dsslib.exceptions.NetworkModuleNotFound;
import dsslib.logs.EventTracer;
import dsslib.scenarios.ScenarioDescriptionLoader;
import dsslib.scenarios.description.LinkSpeedDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class Process implements IProcess {

    private static final Logger logger = LoggerFactory.getLogger(Process.class);

    /** Should be unique in a given simulation **/
    protected String uuid;

    /** Speed is measured in CSpGT: Computation Steps per Global Time **/
    private float speed;

    /** Here we accumulate speed and when gets greater than 1, we reset**/
    private float accumulatedSpeed;

    /** A physical location for the process**/
    private String zone;

    /** We assume everything is enabled **/
    private boolean enabled=true;

    /** The local time of the process **/
    private long localTime = 1;

    /** Event Bus for queueing all events **/
    private dsslib.events.EventBus eventBus;

    /** A set of modules **/
    private Map<String,IModule> modules;

    private NetworkModule networkModule;

    /** Global publisher **/
    private dsslib.events.IPublisher publisher;

    private EventTracer eventTracer;

    private boolean link;

    private boolean gaussian=false;
    private float sdev=0;
    private float mean=0;
    public Process(){
        this(UUID.randomUUID().toString().replaceAll("-",""));
    }

    public Process(String uuid){
        this(uuid,1f);
    }

    //For links....
    public Process(String uuid, LinkSpeedDescription speed){
        this.uuid  = uuid;
        this.link = true;
        initialization();
        String type = speed.getType();
        if(type.equals("gaussian")){
            //10 ms is a very high speed link
            int meanR = (int) speed.getProps().getOrDefault("mean",10);
            int sdevR = (int) speed.getProps().getOrDefault("sdev",5);
            this.mean = 1.0f /meanR;
            this.sdev =  mean*(sdevR/(float)meanR);
            this.speed = mean;
            this.gaussian = true;

        }else{
           this.speed = 1.0f / (float)speed.getProps().getOrDefault("value",1);
        }

    }
    public Process(String uuid, float speed,boolean isLink){
        this.uuid  = uuid;
        this.speed = speed;
        this.link = isLink;
        initialization();
    }
    //For normal processes...
    public Process(String uuid, float speed){
        this.uuid  = uuid;
        this.speed = speed;
        initialization();
    }

    public void initialization(){
        this.publisher = new Publisher();
        this.eventBus = new dsslib.events.EventBus();
        this.modules = new HashMap<>();


        try {
            boolean enabled = true;
            if(ScenarioDescriptionLoader.getScenarioDescription().getMode().isLogsEnabled()) {
                Map<String, Object> trace = ScenarioDescriptionLoader.getScenarioDescription().getMode().getTrace();
                if(this.link && ! (boolean) trace.getOrDefault("links",false)){
                    enabled =false;
                }
                if(enabled)
                    this.eventTracer = new EventTracer(Scheduler.getInstance().getDirectoryService().getTracesPath(), uuid, true);
            }

        } catch (IOException e) {
            logger.error(e.getLocalizedMessage());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
        }

        //The even Init should always be called once the process starts
        //TODO Maybe I will call it after a process is switched back to enable
        trigger(new dsslib.events.Init());

    }
    @Override
    public String getUuid() {
        return uuid;
    }

    @Override
    public void registerComponent(IModule module) {
        logger.trace("Process:"+this.getUuid()+" registers module:"+module.getClass().getName());
        String name = module.getClass().getName();
        if(modules.containsKey(name)){
            logger.info("Module: "+name+" already registered...!");
            return;
        }
        modules.put(name,module);
        if(module instanceof NetworkModule)
            networkModule = (NetworkModule) module;
    }

    @Override
    public boolean step() throws EventHandlerIsNA {
        boolean atLeastOneStepTaken = false;
        accumulatedSpeed += speed;
        if(isReadyForStep()){
            //System.out.println(Scheduler.getInstance().getTime()+">"+localTime+": "+getUuid()+"->"+(1/speed));
            //TODO Here probably we need to see If we are a link and we adhere to
            // a statistical distribution to change our speed....
            if(gaussian){
                try {
                    float gaus = (float) (Scheduler.getInstance().getScenario().random.nextGaussian());
                   // System.out.println(gaus*sdev);
                    speed = gaus*sdev+mean;
                    if(speed<0)
                        speed *=-1;
                    if(speed>1)
                        speed = 1;
                } catch (MissingScenario missingScenario) {
                    missingScenario.printStackTrace();
                }
            }

            trigger(new dsslib.events.Execution());

            while(eventBus.countRemainingEvents() > 0 ){
                atLeastOneStepTaken=true;

                eventBus.broadcast();

                for(IModule component: modules.values()){
                    component.step();
                }

            }

            localTime++;
        }
        return atLeastOneStepTaken;
    }

    public void trigger(dsslib.events.Event event){
        publisher.publish(event, eventBus);
    }

    @Override
    public EventTracer getEventTracer() {
        return eventTracer;
    }

    @Override
    public NetworkModule getNetworkModule() throws NetworkModuleNotFound {
        if(networkModule==null)
            throw new NetworkModuleNotFound(uuid);
        return networkModule;
    }

    @Override
    public boolean containsModule(Class<? extends IModule> module) {
        return modules.values().stream().map(m->m.getClass().getSuperclass().getTypeName())
                .anyMatch(m->module.getTypeName().equals(m));
    }


    @Override
    public IModule getModule(Class<? extends IModule> module) throws MissingModule {
        //TODO Fix this, not necessary to loop now
        Optional<IModule> result =
                modules.values().stream().filter(m->m.getClass().getSuperclass()
                        .getTypeName().equals(module.getTypeName())).findFirst();
        if(!result.isPresent())
            throw new MissingModule(module);
        return result.get();
    }


    @Override
    public void on(Class<? extends dsslib.events.Event> evnt, dsslib.events.EventHandler<? extends dsslib.events.Event> handler) {
        try {
            AbstractModule module = (AbstractModule) getModule(DebugModule.class);
            module.subscribe(handler,evnt);
        } catch (MissingModule missingModule) {
            try {
                DebugModule module = new DebugComponent(this,new HashMap<>());
                module.subscribe(handler,evnt);
                this.registerComponent(module);
            } catch (MissingModule e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void setZone(String zone) {
        this.zone = zone;
    }

    @Override
    public boolean isReadyForStep() {
        boolean result = false;
        if(accumulatedSpeed >= 1){
            result = true;
            accumulatedSpeed = accumulatedSpeed-1;
        }
        return result;
    }

    @Override
    public dsslib.events.EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enable) {
        this.enabled = enable;

    }

    public Map<String, IModule> getModules() {
        return modules;
    }

    @Override
    public long getLocalTime() {
        return localTime;
    }

    @Override
    public void close() throws Exception {
        if(eventTracer!=null){
            logger.trace(getUuid()+": Closing trace file");

            String trace = getEventBus().getUnprocessedTrace();
            eventTracer.flush("\n# Unprocessed Events:\n");
            if(!trace.isEmpty())
                eventTracer.flush(trace);
        }
    }

    public String getZone() {
        return zone;
    }

    @Override
    public String toString() {
        return uuid;
    }
}
