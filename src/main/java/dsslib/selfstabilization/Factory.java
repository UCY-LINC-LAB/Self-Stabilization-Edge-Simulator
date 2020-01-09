package dsslib.selfstabilization;

import dsslib.process.IProcess;
import dsslib.process.Process;
import dsslib.components.healthcheck.HealthCheckComponent;
import dsslib.components.healthcheck.HealthCheckModule;
import dsslib.components.networking.*;
import dsslib.components.selfstabilization.*;
import dsslib.components.timers.PeriodicTimerComponent;
import dsslib.components.timers.PeriodicTimerModule;
import dsslib.components.timers.SimpleTimer;
import dsslib.scheduler.Scheduler;
import dsslib.exceptions.MissingModule;
import dsslib.exceptions.MissingScenario;
import dsslib.exceptions.NetworkModuleNotFound;
import dsslib.exceptions.SimulationException;
import dsslib.scenarios.environment.EnvironmentEvent;
import dsslib.scenarios.environment.EventType;

import java.util.*;
import java.util.stream.Collectors;

public class Factory {

    private static Factory instance;

    public static final Random random = new Random(2);

    /**These are global **/
    private Map<String, IProcess> processes = new HashMap<>();
    private Map<String, SSCloudModule> clouds = new HashMap<>();
    private Map<String, SSCloudletModule> cloudlets = new HashMap<>();
    private Map<String, SSIoTModule> iots = new HashMap<>();

    /**Used for unique identifiers**/
    private static int cloudsIds;
    private static int cloudletsIds;
    private static int iotsIds;

    //Holds for each cloudlet all iotsIds
    private Map<String,Set<String>> cloudletResponsibilities = new HashMap<>();

    //Holds for each cloudlet all iotsIds
    private Map<String,List<String>> iotsCloudletList = new HashMap<>();

    /** In this setup shared memory is accessible by cloudsIds and cloudletsIds. **/
    public  IProcess sharedMemory;

    private Factory() throws MissingModule {
        sharedMemory = new Process("sharedMemory",1f);
        sharedMemory.registerComponent(new NetworkComponent(sharedMemory,new LinkedHashMap<String, Object>(){{
            put("receiveBandwidth",1000);
            put("transmitBandwidth",1000);
        } }));
        sharedMemory.registerComponent(new SharedRegisterComponent(sharedMemory));
        Scheduler.getInstance().register(sharedMemory,true);

    }


    /**
     * The convention is that that the name is cloud_{i}, where i is a unique monotonic integer
     * @param speed
     * @param enabledTime
     * @param periodicity
     * @param receiveBandwidth
     * @param transmitBandwidth
     * @return
     * @throws NetworkModuleNotFound
     */
    public IProcess createCloud(float speed, long enabledTime, int periodicity, int receiveBandwidth, int transmitBandwidth) throws NetworkModuleNotFound, MissingModule, MissingScenario {
        String id = "cloud_"+(++cloudsIds);
        IProcess process = new Process(id, speed);

        /** Networking Module **/
        process.registerComponent(new NetworkComponent(process,new LinkedHashMap<String, Object>(){{
            put("receiveBandwidth",receiveBandwidth);
            put("transmitBandwidth",transmitBandwidth);
        } }));
        /** SimpleTimer required by healthcheck and periodicity module**/
        SimpleTimer simpleTimer = new SimpleTimer(process);
        process.registerComponent(simpleTimer);

        /**HealthCheck Module **/
        HealthCheckComponent healthCheckComponent = new HealthCheckComponent(process);
        process.registerComponent(healthCheckComponent);

        /**Periodicity Module**/

        PeriodicTimerComponent periodicTimerComponent = new PeriodicTimerComponent(process);
        process.registerComponent(periodicTimerComponent);

        /**Register **/
        SharedRegisterModule sharedRegister = new SharedRegisterComponent(process);
        process.registerComponent(sharedRegister);
        //Grant access to shared memory
        registerSharedMemory(process);

        SSCloudModule cloudModule = new SSCloudComponent(process,new LinkedHashMap<String, Object>(){{put("periodicity",periodicity);}});
        process.registerComponent(cloudModule);

        Scheduler.getInstance().register(process,true);

        //Initialize state
        clouds.put(id,cloudModule);
        processes.put(id,process);

        process.setEnabled(false);
        Scheduler.getInstance().getScenario().addEvent(new EnvironmentEvent(enabledTime, EventType.ENABLE_PROCESS,id));
        return process;
    }

    public IProcess createCloudlet(float speed,long enabledTime, int periodicity, int receiveBandwidth, int transmitBandwidth) throws NetworkModuleNotFound, MissingModule, MissingScenario {

        String id = "cloudlet_"+(++cloudletsIds);

        IProcess process = new Process(id, speed);


        /** Networking **/
        process.registerComponent(new NetworkComponent(process,new LinkedHashMap<String, Object>(){{
            put("receiveBandwidth",receiveBandwidth);
            put("transmitBandwidth",transmitBandwidth);
        } }));

        /** SimpleTimer required by healthcheck and periodicity module**/
        SimpleTimer simpleTimer = new SimpleTimer(process);
        process.registerComponent(simpleTimer);

        //Health Check Module
        HealthCheckModule healthCheckComponent = new HealthCheckComponent(process);
        process.registerComponent(healthCheckComponent);

        /**Periodicity Module**/
        PeriodicTimerModule periodicTimerComponent = new PeriodicTimerComponent(process);
        process.registerComponent(periodicTimerComponent);

        /**Register Module **/
        SharedRegisterModule sharedRegister = new SharedRegisterComponent(process);
        process.registerComponent(sharedRegister);
        //Grant access to shared memory
        registerSharedMemory(process);
        Scheduler.getInstance().register(process,true);


        SSCloudletModule cloudletModule = new SSCloudletComponent(process,
                new LinkedHashMap<String, Object>(){{put("periodicity",periodicity);}});
        process.registerComponent(cloudletModule);

        //A list for the IoT the cloud should be responsible
        cloudletResponsibilities.put(process.getUuid(),new LinkedHashSet<>());

        //Initialize state
        cloudlets.put(id,cloudletModule);
        processes.put(id,process);

        process.setEnabled(false);
        Scheduler.getInstance().getScenario().addEvent(new EnvironmentEvent(enabledTime, EventType.ENABLE_PROCESS,id));

        return process;
    }

    public IProcess createIoT (float speed, long enabledTime,int periodicity,int modelUpdatePeriodicity, int timeout) throws MissingModule, MissingScenario {
        String id = "iot_"+(++iotsIds);
        IProcess process = new Process(id, speed);


        /** Required by Periodicity Module **/
        process.registerComponent(new SimpleTimer(process));

        /**Periodicity Module**/
        PeriodicTimerComponent periodicTimerComponent = new PeriodicTimerComponent(process);
        process.registerComponent(periodicTimerComponent);




        /** Required by HealthCheck **/
        process.registerComponent(new NetworkComponent(process,new LinkedHashMap<String, Object>(){{
            put("receiveBandwidth",1);
            put("transmitBandwidth",1);
        } }));

        Scheduler.getInstance().register(process,true);
        iotsCloudletList.put(process.getUuid(),new LinkedList<>());

        SSIoTModule ioTModule = new SSIoTComponent(process,new LinkedHashMap<String,Object>(){{
            put("periodicity",periodicity);
            put("modelUpdatePeriodicity",modelUpdatePeriodicity);
            put("timeout",timeout);
        } });
        process.registerComponent(ioTModule);

        //Initialize state
        processes.put(id,process);
        iots.put(id,ioTModule);

        process.setEnabled(false);
        Scheduler.getInstance().getScenario().addEvent(new EnvironmentEvent(enabledTime, EventType.ENABLE_PROCESS,id));

        return process;
    }

    public void registerSharedMemory(IProcess process) throws NetworkModuleNotFound, MissingModule {
        IProcess linkToSharedMemory = new Process(process.getUuid()+"_sharedMemory");
        NetworkModule network = process.getNetworkModule();
        network.registerChannel(linkToSharedMemory);
        linkToSharedMemory.registerComponent(new ChannelComponent(linkToSharedMemory,new LinkedHashMap<String,Object>(){{
            put("from",process);
            put("to",sharedMemory);
            put("bandwidth",1000);
        }}));
        Scheduler.getInstance().register(linkToSharedMemory,false);

        IProcess linkToSharedMemory2 = new Process("sharedMemory_"+process.getUuid());
        NetworkModule network2 = sharedMemory.getNetworkModule();
        network2.registerChannel(linkToSharedMemory2);
        linkToSharedMemory2.registerComponent(new ChannelComponent(linkToSharedMemory2,new LinkedHashMap<String,Object>(){{
            put("from",sharedMemory);
            put("to",process);
            put("bandwidth",1000);
        }}));
        Scheduler.getInstance().register(linkToSharedMemory2,false);

    }

    public void createDuplexLink(IProcess p1, IProcess p2,float p1p2Speed, int p1p2Bandwidth, float p2p1Speed, int p2p1Bandwidth) throws NetworkModuleNotFound, MissingModule {

        IProcess p1top2 = new Process(p1.getUuid()+"_"+p2.getUuid(),p1p2Speed);
        NetworkModule networkP1 = p1.getNetworkModule();
        networkP1.registerChannel(p1top2);
        p1top2.registerComponent(new ChannelComponent(p1top2,new LinkedHashMap<String,Object>(){{
            put("from",p1);
            put("to",p2);
            put("bandwidth",p1p2Bandwidth);
        }}));
        Scheduler.getInstance().register(p1top2,false);

        IProcess p2top1 = new Process(p2.getUuid()+"_"+p1.getUuid(),p2p1Speed);
        NetworkModule networkP2 = p2.getNetworkModule();
        networkP2.registerChannel(p2top1);
        p2top1.registerComponent(new ChannelComponent(p2top1,new LinkedHashMap<String,Object>(){{
            put("from",p2);
            put("to",p1);
            put("bandwidth",p2p1Bandwidth);
        }}));
        Scheduler.getInstance().register(p2top1,false);

        //Here we add the iot that the cloudlet is responsible
        if(p1.containsModule(SSCloudletModule.class) && p2.containsModule(SSIoTModule.class) ){
            Set<String> iots = cloudletResponsibilities.get(p1.getUuid());
            iots.add(p2.getUuid());

            //TODO consider priority
            List<String> cloudlets = iotsCloudletList.get(p2.getUuid());
            cloudlets.add(p1.getUuid());

        }else if(p2.containsModule(SSCloudletModule.class) && p1.containsModule(SSIoTModule.class) ){

            Set<String> iots = cloudletResponsibilities.get(p2.getUuid());
            iots.add(p1.getUuid());

            //TODO consider priority
            List<String> cloudlets = iotsCloudletList.get(p1.getUuid());
            cloudlets.add(p2.getUuid());

        }

    }

    /**
     * See algorithm 3
     * @param process
     * @return
     */
    public Set<String> reachableIoT(IProcess process) {
        return cloudletResponsibilities.getOrDefault(process.getUuid(),new LinkedHashSet<>());

    }



    int rands[]={100,50,60,35};

    /**
     * See algorithm 1
     * @param iot
     * @return
     */
    public PriorityQueue<CloudletWithPriority> cloudletList(String iot) {
        PriorityQueue<CloudletWithPriority> queue = new PriorityQueue<>();
        List<String> cloudlets =  iotsCloudletList.getOrDefault(iot, new LinkedList<>());
        int c=0;
        for(String cloudlet : cloudlets){
            queue.add(new CloudletWithPriority(cloudlet, (double) rands[c]));
            c = (c + 1) % rands.length;
        }
        return queue;
    }

    /**
    public synchronized static Factory getInstance(){
        if(instance == null){
            try {
                instance = new Factory();
            } catch (MissingModule missingModule) {
                missingModule.printStackTrace();
            }
            return instance;
        }
        return instance;
    }
     **/

    /**
     * Implementation of suspectedIoT in Algorithm 1
     * TODO For now we return an empty set
     * @param iots
     * @return
     */
    public Set<String> cloudSuspectedIots(Set<String> iots) {
        iots.removeIf(curr -> !suspectedHosts.contains(curr));
        return  iots;
    }

    /**
     * Implementation of suspectedCloudlet in Algorithm 1
     *
     * TODO For now we return an empty set
     * @param cloudlets
     * @return
     */
    public Set<String> cloudSuspectedCloudlet(Set<String> cloudlets) {
        cloudlets.removeIf(curr -> !suspectedHosts.contains(curr));
        return  cloudlets;
    }

    /**
     * Implementation of suspectedCloudlet in Algorithm 3
     *
     * @param cloudlets
     * @return
     */
    public Set<String> cloudletSuspectedCloudlet(Set<String> cloudlets) {
        cloudlets.removeIf(curr -> !suspectedHosts.contains(curr));
        return  cloudlets;
    }
    /**
     *TODO Random for now
     * @param cloudlets
     * @return
     */
    public String electLeader(Set<String> cloudlets) throws SimulationException {
        int size = cloudlets.size();
        if(size == 0)
            throw new SimulationException("Couldn't elect leader. Set is empty");

        int index = random.nextInt(size);
        Optional<String> cloudlet = cloudlets.stream().sorted().skip(index).findFirst();

        if(cloudlet.isPresent())
            return cloudlet.get();
        else
            throw new SimulationException("Couldn't elect leader. Didn't find a clodulet");
    }

    public String getState() throws MissingModule {
        StringBuilder sb = new StringBuilder();

        SharedRegisterModule sharedRegister = (SharedRegisterModule) sharedMemory.getModule(SharedRegisterModule.class);
        String records = sharedRegister.records.entrySet().stream().map(e->e.getKey()+":"+e.getValue()).collect(Collectors.joining("\n\t\t"));

        sb.append("State:");
        sb.append("\n\tSharedRegister:");
        sb.append("\n\t\t"+records);
        sb.append("\n\tCloudlets:");
        sb.append("\n\t\t"+cloudlets.entrySet().stream().map(e->e.getKey()+":"+e.getValue()).collect(Collectors.joining("\n\t\t")));
        sb.append("\n\tIoTs:");
        sb.append("\n\t\t"+iots.entrySet().stream().map(e->e.getKey()+":"+e.getValue()).collect(Collectors.joining("\n\t\t")));
        return sb.toString();
    }

    public Map<String, IProcess> getProcesses() {
        return processes;
    }

    //For Fault tolerance mechanism
    Set<String> suspectedHosts = new LinkedHashSet<>();

    public void addHostToSuspectedList(String host) {
        suspectedHosts.add(host);
    }

    public void removeHostFromSuspectedList(String host) {
        suspectedHosts.remove(host);
    }

    public Set<String> selectGuards(Set<String> set) throws MissingScenario {
        int size = set.size();
        int g = (int) Scheduler.getInstance().getScenario().getProperty("guards",1);
        if(size<g){
            size = g;
        }
        return set.stream().sorted().skip(size-g).collect(Collectors.toSet());
    }
}
