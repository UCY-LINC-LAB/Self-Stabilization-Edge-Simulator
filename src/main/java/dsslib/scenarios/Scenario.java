package dsslib.scenarios;

import dsslib.process.IProcess;
import dsslib.components.networking.NetworkModule;
import dsslib.components.networking.SharedRegisterModule;
import dsslib.scheduler.Scheduler;
import dsslib.exceptions.MissingModule;
import dsslib.exceptions.MissingTopology;
import dsslib.exceptions.NetworkModuleNotFound;
import dsslib.statistics.Statistics;
import dsslib.scenarios.environment.EnvironmentEvent;
import dsslib.scenarios.environment.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class Scenario {
    private static final Logger logger = LoggerFactory.getLogger(Scenario.class);

    /**
     * This specifies all the processes and links in this simulation
     */
    private Topology topology;

    public Random random;

    private SimulationMode mode;

    public SimulationMode getMode() {
        return mode;
    }

    public void setMode(SimulationMode mode) {
        this.mode = mode;
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    public Topology getTopology()  throws MissingTopology {
        return topology;
    }


    Map<String,Object> properties;

    public void setProperties(Map<String,Object> properties) {
        this.properties = properties;
    }

    /** A sequence of events to execute **/
    PriorityQueue<EnvironmentEvent> eventQueue = new PriorityQueue<>();

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public void addEvent(EnvironmentEvent event){
        eventQueue.add(event);
    }

    public List<EnvironmentEvent> getNextAvailable(long globalTime){
        List<EnvironmentEvent> events = new LinkedList<>();
        if(this.eventQueue.isEmpty())
            return events;

        while(!this.eventQueue.isEmpty() && this.eventQueue.peek().getGlobalTime() == globalTime){
            events.add(this.eventQueue.poll());
        }
        return events;
    }

    public void handle(EnvironmentEvent event) throws MissingTopology, MissingModule {
        switch (event.getEventType()){
            case FAIL_CLOUDLET:
                String cloudlet = event.getProcessId();
                IProcess process = this.getTopology().getProcesses().get(cloudlet);
                if(process!=null){
                    process.setEnabled(false);
                    Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
                        put("event",Statistics.Event.PROCESS_FAIL);
                        put("process",process.getUuid());
                        put("zone",process.getZone());
                    }});
                }
                break;
            case ENABLE_PROCESS:
                String id = event.getProcessId();
                process = this.getTopology().getProcesses().get(id);
                if(process!=null) {
                    process.setEnabled(true);
                    Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
                        put("event",Statistics.Event.PROCESS_ENABLED);
                        put("process",process.getUuid());
                        put("zone",process.getZone());
                    }});
                    checkIfShouldEnableCloudlet(process);
                }
                break;
            case FAIL_PROCESS:
                id = event.getProcessId();
                process = this.getTopology().getProcesses().get(id);
                if(process!=null) {
                    process.setEnabled(false);
                    Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
                        put("event",Statistics.Event.PROCESS_FAIL);
                        put("process",process.getUuid());
                        put("zone",process.getZone());
                    }});
                    checkIfShouldEnableCloudlet(process);
                }
                break;
            case FAIL_LINK:
                String from = (String) event.getProperty("p1");
                String to = (String) event.getProperty("p2");
                if(from!=null && to!=null){
                    IProcess p1 = getTopology().getLinks().get(from + "_" + to);
                    IProcess p2= getTopology().getLinks().get(to + "_" + from);
                    if(p1!=null && p2 !=null){
                        p1.setEnabled(false);
                        p2.setEnabled(false);
                        Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
                            put("event",Statistics.Event.LINK_FAILED);
                            put("process",from+"_"+to);
                        }});
                    }

                }
                break;
            case FAIL_LEADER:
                IProcess leader = ((VirtualFogTopology)this.getTopology()).getCurrentLeader();
                if(leader!=null) {
                    leader.setEnabled(false);
                    Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
                        put("event",Statistics.Event.PROCESS_FAIL);
                        put("process",leader.getUuid());
                        put("zone",leader.getZone());
                    }});
                }

                break;
            case ENABLE_PREVIOUS_LEADER:
                IProcess prevLeader = ((VirtualFogTopology)this.getTopology()).getPreviousLeader();
                if(prevLeader!=null) {
                    prevLeader.setEnabled(true);
                    Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
                        put("event",Statistics.Event.PROCESS_ENABLED);
                        put("process",prevLeader.getUuid());
                        put("zone",prevLeader.getZone());
                    }});
                    checkIfShouldEnableCloudlet(prevLeader);
                }
                break;
            case ALL_CLOUDLETS_BELIEVE_THEY_ARE_LEADER:
                int rounds = (int) event.getProperty("rounds");
                VirtualFogTopology virtualFogTopology = Scheduler.getInstance().getVirtualFogTopolgy();
                Set<String> cloudlets = virtualFogTopology.getCloudlets().keySet();
                IProcess sharedMemory = virtualFogTopology.getSharedMemory();
                SharedRegisterModule shr = (SharedRegisterModule) sharedMemory.getModule(SharedRegisterModule.class);
                for(String cl:cloudlets) {
                    shr.makeCloudletBelieveThatIsALeaderForNReads(cl,rounds);
                }
                break;
            case FAIL_CLOUDLETS_ONLY:
                int count = (int) event.getProperty("count");
                Integer comeback = (Integer) event.getProperty("comeback");
                virtualFogTopology = Scheduler.getInstance().getVirtualFogTopolgy();
                List<IProcess> choices = new LinkedList<>(virtualFogTopology.getCloudlets().values());
                Set<String> currentGuards = virtualFogTopology.getCurrentGuards().stream()
                        .map(IProcess::getUuid).collect(Collectors.toSet());
                if(virtualFogTopology.getCurrentLeader()!=null)
                    currentGuards.add(virtualFogTopology.getCurrentLeader().getUuid());
                choices.removeIf(e->currentGuards.contains(e.getUuid()) || !e.isEnabled());
                int size = choices.size();
                if(count>choices.size()){
                    logger.info("There are not enough cloudlets to fail. Failing only: "+size);
                    count = choices.size();
                }
                List<String> choicesList = choices.stream().map(IProcess::getUuid).collect(Collectors.toList());
                Set<String> result = new LinkedHashSet<>();
                Set<Integer> rands = new TreeSet<>();
                while(result.size()<count){
                    int r = getRandom().nextInt(size);
                    if(rands.contains(r))
                        continue;
                    rands.add(r);
                    result.add(choicesList.get(r));
                }
                for(String cl: result){
                    IProcess p =virtualFogTopology.getCloudlets().get(cl);
                    if(p!=null) {
                        p.setEnabled(false);
                        Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
                            put("event",Statistics.Event.PROCESS_FAIL);
                            put("process",p.getUuid());
                            put("zone",p.getZone());
                        }});
                        if(comeback!=null){

                            EnvironmentEvent ev = new EnvironmentEvent(event.getGlobalTime()+comeback,
                                    EventType.ENABLE_PROCESS, cl);
                            this.addEvent(ev);
                            logger.info("Registered event: " + ev);
                        }
                    }



                }

                break;
            case FAIL_GUARDS:
                count = (int) event.getProperty("count");
                comeback = (Integer) event.getProperty("comeback");
                virtualFogTopology = Scheduler.getInstance().getVirtualFogTopolgy();
                currentGuards = virtualFogTopology.getCurrentGuards().stream()
                        .map(IProcess::getUuid).collect(Collectors.toSet());
                int total = Math.min(count,currentGuards.size());
                int c=0;
                for(String cl: currentGuards) {
                    IProcess p = virtualFogTopology.getCloudlets().get(cl);
                    if (p != null) {
                        p.setEnabled(false);
                        Statistics.getInstance().record("selfStabilization", new HashMap<String, Object>() {{
                            put("event", Statistics.Event.PROCESS_FAIL);
                            put("process", p.getUuid());
                            put("zone", p.getZone());
                        }});
                        if (comeback != null) {

                            EnvironmentEvent ev = new EnvironmentEvent(event.getGlobalTime() + comeback,
                                    EventType.ENABLE_PROCESS, cl);
                            this.addEvent(ev);
                            logger.info("Registered event: " + ev);
                        }
                        c++;
                        if(c==total){
                            break;
                        }
                    }
                }
                break;
            case ENABLE_ALL_RANDOM:
                int start = (int) event.getProperty("from");
                int end = (int) event.getProperty("to");
                int dist = end-start;
                Collection<String> proccesses = Scheduler.getInstance().getAddressResolutionTable().keySet();
                proccesses.remove("sharedMemory");
                proccesses.remove("cloud");
                for(String p: proccesses) {
                    Scheduler.getInstance().getAddressResolutionTable().get(p).setEnabled(false);
                    int x = (int) (getRandom().nextInt(dist)+start+event.getGlobalTime()+1);
                    EnvironmentEvent ev = new EnvironmentEvent(x, EventType.ENABLE_PROCESS, p);
                    this.addEvent(ev);
                    logger.info("Registered event: " + ev);
                }

                break;
            case FAIL_LINKS_IOT_TO_CLOUDLET:
                Integer f = (Integer) event.getProperty("from");
                Integer t = (Integer) event.getProperty("to");
                count = (Integer) event.getProperty("count");
                List<String> regions = (List<String>) event.getProperty("regions");
                virtualFogTopology = Scheduler.getInstance().getVirtualFogTopolgy();
                for(String region:regions){
                    Set<IProcess> iots = virtualFogTopology.getIotZones().get(region);
                    //Ignoring false regions
                    if(iots==null)
                        continue;
                    int x=0;
                    int tat = (int) (getRandom().nextInt(t-f)+t+event.getGlobalTime()+1);
                    for(IProcess p : iots){

                        try {
                            NetworkModule networkModule = p.getNetworkModule();
                            Set<String> pp2 = networkModule.connectedProcesses().stream().filter(e->e.contains("cloudlet")).collect(Collectors.toSet());
                            for(String p2: pp2){
                                EnvironmentEvent ev = new EnvironmentEvent(tat,EventType.FAIL_LINK,p.getUuid());
                                ev.addProperty("p1",p.getUuid());
                                ev.addProperty("p2",p2);
                                addEvent(ev);
                                logger.info("Registered event: " + ev);
                            }

                            //ch
                        } catch (NetworkModuleNotFound networkModuleNotFound) {
                            networkModuleNotFound.printStackTrace();
                        }
                        x++;
                        if(x>count)
                            break;
                    }

                }
                break;
            case FAIL_IOTS_ONLY:
                count = (int) event.getProperty("count");
                comeback = (Integer) event.getProperty("comeback");
                virtualFogTopology = Scheduler.getInstance().getVirtualFogTopolgy();
                choices = new LinkedList<>(virtualFogTopology.getIots().values());
                choices.removeIf(e->!e.isEnabled());
                size = choices.size();
                if(count>choices.size()){
                    logger.info("There are not enough IoTs to fail. Failing only: "+size);
                    count = choices.size();
                }
                choicesList = choices.stream().map(IProcess::getUuid).collect(Collectors.toList());
                result = new LinkedHashSet<>();
                rands = new TreeSet<>();
                while(result.size()<count){
                    int r = getRandom().nextInt(size);
                    if(rands.contains(r))
                        continue;
                    rands.add(r);
                    result.add(choicesList.get(r));
                }
                for(String cl: result){
                    IProcess p =virtualFogTopology.getIots().get(cl);
                    if(p!=null) {
                        p.setEnabled(false);
                        Statistics.getInstance().record("selfStabilization",new HashMap<String,Object>(){{
                            put("event",Statistics.Event.PROCESS_FAIL);
                            put("process",p.getUuid());
                            put("zone",p.getZone());
                        }});
                        if(comeback!=null){
                            EnvironmentEvent ev = new EnvironmentEvent(event.getGlobalTime()+comeback,
                                    EventType.ENABLE_PROCESS, cl);
                            this.addEvent(ev);
                            logger.info("Registered event: " + ev);
                        }
                    }



                }

                break;
        }

    }

    private void checkIfShouldEnableCloudlet(IProcess process) {
        //TODO Tommorow. Fix the issue, when a cloudlet is added an # of guards havne't reached yet
        /**
        guards = selectGuards(candidates);
        guards = Scheduler.getInstance().getVirtualFogTopolgy().selectGuards(set);
        for(String s:set) {
            trigger(new HealthCheckModule.Stop(s));
            trigger(new HealthCheckModule.Start(s, checkPeriodForLeader));
            lCloudlets.put(s, checkPeriodForLeader);
        }
         **/
    }


    public Random getRandom() {
        return random;
    }

    public Object getProperty(String key){
        return properties.get(key);
    }
    public Object getProperty(String key,Object v){
        return properties.getOrDefault(key,v);
    }
    public void setProperty(String key,Object value){
        properties.put(key,value);
    }


    @Override
    public String toString() {
        return "Scenario{" +
                "topology=" + topology +
                ", properties=" + properties +
                ", eventQueue=" + eventQueue +
                '}';
    }
}
