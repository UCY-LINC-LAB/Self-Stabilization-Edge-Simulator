
package dsslib.scenarios;

import dsslib.process.IProcess;
import dsslib.process.Process;
import dsslib.components.networking.*;
import dsslib.components.selfstabilization.SSCloudModule;
import dsslib.components.selfstabilization.SSCloudletLeaderModule;
import dsslib.components.selfstabilization.SSCloudletModule;
import dsslib.components.selfstabilization.SSIoTModule;
import dsslib.scheduler.Scheduler;
import dsslib.exceptions.*;
import dsslib.selfstabilization.CloudletWithPriority;
import dsslib.selfstabilization.cloudlets.Replica;
import dsslib.statistics.Statistics;
import dsslib.utilities.O;
import dsslib.utilities.Tuple2;
import dsslib.utilities.Tuple3;
import dsslib.scenarios.description.LinkSpeedDescription;
import dsslib.scenarios.description.ZonesDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A virtual fog topology
 */
public class VirtualFogTopology extends Topology{

    private static final Logger logger = LoggerFactory.getLogger(VirtualFogTopology.class);

    //For Fault tolerance mechanism
    Set<String> suspectedHosts = new LinkedHashSet<>();

    /*A singleton*/
    private IProcess cloud;
    private SSCloudModule cloudModule;

    private String cloudIP = "cloud";

    /*For unique IDs*/
    private int cloudletsIds;
    private int iotsIds;

    /** The set of cloudlets **/
    private Map<String,IProcess> cloudlets = new HashMap<>();

    /** The set of cloudlets zones **/
    private Map<String, Set<IProcess>> cloudletZones = new HashMap<>();

    /** The set of iotZOnes **/
    private Map<String, Set<IProcess>> iotZones = new HashMap<>();

    /** The set of Iots **/
    private Map<String,IProcess> iots = new HashMap<>();

    //Holds for each cloudlet all iotsIds
    private Map<String,Set<String>> cloudletResponsibilities = new HashMap<>();

    //Holds for each iot a Prioritized list of cloudlets
    private Map<String,List<String>> iotsCloudletList = new HashMap<>();

    /** In this setup shared memory is accessible by cloudsIds and cloudletsIds. **/
    public  IProcess sharedMemory;

    /** Current Leader **/
    private IProcess currentLeader;
    private IProcess previousLeader;
    private Set<IProcess> currentGuards = new HashSet<>();


    public VirtualFogTopology() throws MissingModule, NoRequiredModuleFound {
        sharedMemory = new Process("sharedMemory",1f);
        sharedMemory.registerComponent(new NetworkComponent(sharedMemory,new LinkedHashMap<String, Object>(){{
            put("receiveBandwidth",1000);
            put("transmitBandwidth",1000);
        } }));
        sharedMemory.registerComponent(new SharedRegisterComponent(sharedMemory));
        registerProcess(sharedMemory);
    }

    /**
     * The cloud is a single instance
     * The cloud is lazy loaded
     * @return
     */
    public synchronized IProcess createCloud(float speed) throws NetworkModuleNotFound, MissingModule, MissingScenario, NoRequiredModuleFound {
        if(cloud==null){
            this.cloud = new Process(cloudIP,speed);
        }
        return cloud;
    }

    public IProcess createCloudlet(float speed, String zone) throws NetworkModuleNotFound, MissingModule, MissingScenario, NoRequiredModuleFound {
        String id = "cloudlet_" + (++cloudletsIds);
        IProcess process = new Process(id, speed);
        process.setZone(zone);

        //Add the cloudlet in zone
        Set<IProcess> cloudletsZones = cloudletZones.getOrDefault(zone,new LinkedHashSet<>());
        cloudletsZones.add(process);
        cloudletZones.putIfAbsent(zone,cloudletsZones);
        cloudlets.put(process.getUuid(),process);
        return process;
    }

    public IProcess createIoT (float speed, String zone) throws MissingModule, MissingScenario, NoRequiredModuleFound {
        String id = "iot_"+(++iotsIds);
        IProcess process = new Process(id, speed);
        process.setZone(zone);

        //Initialize state
        this.iots.put(id,process);
        Set<IProcess> iots = iotZones.getOrDefault(zone,new LinkedHashSet<>());
        iots.add(process);
        iotZones.putIfAbsent(zone,iots);
        return process;
    }

    /**
     * Creates two links. One from p1 to p2 and another from p2 to p1.
     * The speed is the same, however bandiwdth can differ.
     * @param p1
     * @param p2
     * @param speed
     * @param p1p2Bandwidth
     * @param p2p1Bandwidth
     * @throws NetworkModuleNotFound
     * @throws MissingModule
     * @throws ProcessNotFound
     * @throws NoRequiredModuleFound
     */
    public void link(String p1, String p2, LinkSpeedDescription speed, int p1p2Bandwidth, int p2p1Bandwidth) throws NetworkModuleNotFound, MissingModule, ProcessNotFound, NoRequiredModuleFound {

        //TODO Think what we should do
        IProcess process1 = getProcesses().get(p1);
        if(process1 ==null)
            throw new ProcessNotFound(p1);

        IProcess process2 = getProcesses().get(p2);
        if(process2 ==null)
            throw new ProcessNotFound(p2);


        //Create a new link from p1 to p2
        String idp1 = process1.getUuid()+"_"+process2.getUuid();
        IProcess p1top2 = new Process(idp1,speed);

        NetworkModule networkP1 = process1.getNetworkModule();
        networkP1.registerChannel(p1top2);
        p1top2.registerComponent(new ChannelComponent(p1top2,new LinkedHashMap<String,Object>(){{
            put("from",process1);
            put("to",process2);
            put("bandwidth",p1p2Bandwidth);
        }}));


        //Create a new link from p2 to p1
        String idp2 = process2.getUuid()+"_"+process1.getUuid();
        IProcess p2top1 = new Process(idp2,speed);
        NetworkModule networkP2 = process2.getNetworkModule();
        networkP2.registerChannel(p2top1);
        p2top1.registerComponent(new ChannelComponent(p2top1,new LinkedHashMap<String,Object>(){{
            put("from",process2);
            put("to",process1);
            put("bandwidth",p2p1Bandwidth);
        }}));

        registerProcess(p1top2);
        registerProcess(p2top1);

    }

    public void registerToSharedMemory(IProcess process) throws NetworkModuleNotFound, MissingModule, NoRequiredModuleFound {
        IProcess linkToSharedMemory = new Process(process.getUuid()+"_sharedMemory",1,true);
        NetworkModule network = process.getNetworkModule();
        network.registerChannel(linkToSharedMemory);

        linkToSharedMemory.registerComponent(new ChannelComponent(linkToSharedMemory,new LinkedHashMap<String,Object>(){{
            put("from",process);
            put("to",sharedMemory);
            put("bandwidth",1000);
        }}));
        registerProcess(linkToSharedMemory);

        IProcess linkToSharedMemory2 = new Process("sharedMemory_"+process.getUuid(),1,true);
        NetworkModule network2 = sharedMemory.getNetworkModule();
        network2.registerChannel(linkToSharedMemory2);
        linkToSharedMemory2.registerComponent(new ChannelComponent(linkToSharedMemory2,new LinkedHashMap<String,Object>(){{
            put("from",sharedMemory);
            put("to",process);
            put("bandwidth",1000);
        }}));

        registerProcess(linkToSharedMemory2);

    }

    public void setCloudModule(SSCloudModule cloudModule) {
        this.cloudModule = cloudModule;
    }

    public SSCloudModule getCloudModule() {
        return cloudModule;
    }

    public Map<String, Set<IProcess>> getCloudletZones() {
        return cloudletZones;
    }

    public Map<String, Set<IProcess>> getIotZones() {
        return iotZones;
    }
    //TODO Change the folowing

    /**
     * See algorithm 3
     * @param process
     * @return
     */
    public Set<String> reachableIoT(IProcess process) {
        return new TreeSet<>(cloudletResponsibilities.getOrDefault(process.getUuid(),new LinkedHashSet<>()));

    }



    int rands[]={100,50,60,35};

    /**
     * TODO Load balancing here
     * @param iot
     * @return
     */
    public PriorityQueue<CloudletWithPriority> cloudletList(String iot) {
        PriorityQueue<CloudletWithPriority> queue = new PriorityQueue<>();
        List<String> cloudlets =  iotsCloudletList.getOrDefault(iot, new LinkedList<>());
        cloudlets = new LinkedList<>(cloudlets);
        //Faulty cloudlets are removed  from the list
        cloudlets.removeIf(e->!getCloudlets().get(e).isEnabled());

        int c=0;
        for(String cloudlet : cloudlets){
            queue.add(new CloudletWithPriority(cloudlet, (double) rands[c]));
            c = (c + 1) % rands.length;
        }
        return queue;
    }


    public IProcess getSharedMemory() {
        return sharedMemory;
    }

    /**
     * Implementation of suspectedIoT in Algorithm 1
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
     * @param cloudlets
     * @return
     */
    public String electLeader(Set<String> cloudlets) throws SimulationException, MissingScenario {
        int size = cloudlets.size();
        if(size == 0)
            throw new SimulationException("Couldn't elect leader. Set is empty");

        int index = Scheduler.getInstance().getScenario().getRandom().nextInt(size);
        Optional<String> cloudlet = cloudlets.stream().sorted().skip(index).findFirst();

        if(cloudlet.isPresent()) {
            //TODO Update statistics
            String leaderID = cloudlet.get();
            previousLeader = currentLeader;
            currentLeader = getProcesses().get(leaderID);

            //SSCloudletLeaderModule m = (SSCloudletLeaderModule) currentLeader.getModule(SSCloudletLeaderModule.class);
            currentLeader.trigger(new SSCloudletLeaderModule.ResetLeaderShip());

            logger.info("Leader now is: "+leaderID);

            Statistics.getInstance().record("selfStabilization",new HashMap<String, Object>(){{
                put("event", Statistics.Event.NEW_LEADER);
                put("process",currentLeader.getUuid());
            }});

            return leaderID;
        } else {
            throw new SimulationException("Couldn't elect leader. Didn't find a clodulet");
        }
    }

    public Map<String,Object> getState() {
        StringBuilder sb = new StringBuilder();
        Map<String,Object> result = new HashMap<>();

        SharedRegisterModule sharedRegister = null;
        try {
            sharedRegister = (SharedRegisterModule) sharedMemory.getModule(SharedRegisterModule.class);
        } catch (MissingModule missingModule) {
            missingModule.printStackTrace();
            return new HashMap<>();
        }
        /**
        try {
            result.put("cloud",cloud.getModule(SSCloudModule.class).getState());
        } catch (MissingModule missingModule) {
            missingModule.printStackTrace();
            return null;
        }
         **/
        result.put("sharedRegister",sharedRegister.getState());
        Map<String,Map<String,Object>> cloudlets = this.cloudlets.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e-> {
                    try {
                        return e.getValue().getModule(SSCloudletModule.class).getState();
                    } catch (MissingModule missingModule) {
                        missingModule.printStackTrace();
                        return null;
                    }
                }));
        result.put("cloudlets",cloudlets);
        sb.append("\n\tIoTs:");
        sb.append("\n\t\t"+iots.entrySet().stream().map(e->e.getKey()+":"+e.getValue()).collect(Collectors.joining("\n\t\t")));

        return result;
        //return sb.toString();
    }


    public void addHostToSuspectedList(String host) {
        suspectedHosts.add(host);
    }

    public void removeHostFromSuspectedList(String host) {
        suspectedHosts.remove(host);
    }

    public Set<String> selectGuards(Set<String> potentialGuards, Set<String> guards) throws MissingScenario {
        //From this set make sure that it is not a leader
        if(currentLeader!=null){
            potentialGuards.remove(currentLeader.getUuid());
        }
        //Need to update guards
        for(String g: guards){
            currentGuards.add(cloudlets.get(g));
        }
        currentGuards.removeIf(g->!guards.contains(g.getUuid()));

        for(IProcess currentGuard : currentGuards){
            potentialGuards.remove(currentGuard.getUuid());
        }

        int size = potentialGuards.size();
        int g = (int) Scheduler.getInstance().getScenario().getProperty("guards",1);
        if(size<g){
            size = g;
            return new LinkedHashSet<>();
        }
        Set<String> pcs = potentialGuards.stream().sorted().skip(size - g).collect(Collectors.toSet());
        for(String s: pcs){
            IProcess p = getCloudlets().get(s);
            currentGuards.add(p);
            Statistics.getInstance().record("selfStabilization",new HashMap<String, Object>(){{
                put("event", Statistics.Event.ELECTED_AS_GUARD);
                put("process",s);
                put("zone",p.getZone());
            }});
        }
        return pcs;
    }

    public String getStateStr() {
        StringBuilder sb = new StringBuilder();
        O result = new O();

        SharedRegisterModule sharedRegister = null;
        try {
            sharedRegister = (SharedRegisterModule) sharedMemory.getModule(SharedRegisterModule.class);
        } catch (MissingModule missingModule) {
            missingModule.printStackTrace();
        }
        String records = sharedRegister.records.entrySet().stream().map(e->e.getKey()+":"+e.getValue()).collect(Collectors.joining("\n\t\t"));
        sb.append("\n\tSharedRegister:");
        sb.append("\n\t\t"+records);
        sb.append("\n\tCloudlets:");
        sb.append("\n\t\t"+cloudlets.entrySet().stream().map(e->e.getKey()+":"+e.getValue()).collect(Collectors.joining("\n\t\t")));
        sb.append("\n\tIoTs:");
        sb.append("\n\t\t"+iots.entrySet().stream().map(e->e.getKey()+":"+e.getValue()).collect(Collectors.joining("\n\t\t")));

        return sb.toString();
    }

    @Override
    public String toString() {
        return "VirtualFogTopology{" +
                "cloud=" + cloud +
                ", cloudletsIds=" + cloudletsIds +
                ", iotsIds=" + iotsIds +
                ", cloudlets=" + cloudlets +
                ", cloudletZones=" + cloudletZones +
                ", iots=" + iots +
                ", cloudletResponsibilities=" + cloudletResponsibilities +
                ", iotsCloudletList=" + iotsCloudletList +
                ", sharedMemory=" + sharedMemory +
                ", rands=" + Arrays.toString(rands) +
                ", suspectedHosts=" + suspectedHosts +
                '}';
    }

    public String getCloudIP() {
        return cloudIP;
    }


    public IProcess getCloud() {
        return cloud;
    }

    public Map<String, IProcess> getIots() {
        return iots;
    }

    public Map<String, IProcess> getCloudlets() {
        return cloudlets;
    }

    public Map<String, Set<String>> getCloudletResponsibilities() {
        return cloudletResponsibilities;
    }

    /**
     * Load balancing is here
     * @param cloudlet
     * @param iot
     */
    public void addIoTtoCloudletResponsibility(String cloudlet, String iot){
        Set<String> def = cloudletResponsibilities.getOrDefault(cloudlet, new HashSet<>());
        def.add(iot);
        cloudletResponsibilities.putIfAbsent(cloudlet,def);

        List<String> orDefault = iotsCloudletList.getOrDefault(iot, new LinkedList<>());
        orDefault.add(cloudlet);
        iotsCloudletList.putIfAbsent(iot,orDefault);


    }

    /**
     * Return faulty processes
     * This failure detector is immediate. When a processes is not enabled it means it has fail.
     * @return
     */
    public Set<String> failureDetector() {
        Set<String> set = new HashSet<>();

        getProcesses().forEach((key, value) -> {
            if (value.isEnabled())
                set.add(key);
        });
        return set;
    }
    /**
     * Return faulty processes
     * This failure detector is immediate. When a processes is not enabled it means it has fail.
     * @return
     */
    public Set<String> failureDetectorAboutCloudlets() {
        Set<String> set = new HashSet<>();

        getCloudlets().forEach((key, value) -> {
            if (value.isEnabled())
                set.add(key);
        });
        return set;
    }






    /**Implemantations of state **/


    /**
     * This function is executed by the leader and the guards.
     *
     * @param rep the rep to set the state
     * @param msg The msgs to aggregate
     * @return
     */
    public Object apply(Replica rep, Map<String, Map<String, Tuple2>> msg, Set<String> active) {
        //Here we should decide whether a partitioning per region should be used...
        //TODO FIx state now. As the aggregate is a list....
        //String state = "state";
        boolean same = true;
        if(msg.isEmpty()) {
            //No change on state...
            return rep.getState();
        }

        //For each cloud we store the values we believe
        Map<String,TreeSet<Tuple2>> consolidatedInfo = new HashMap<>();

        for(Map<String, Tuple2> m: msg.values()){

            if(m==null)
                continue;

            for(Map.Entry<String,Tuple2> info: m.entrySet()){
                String cloudlet = info.getKey();
                if(active.contains(cloudlet)) {
                    TreeSet<Tuple2> set = consolidatedInfo.getOrDefault(cloudlet, new TreeSet<>());
                    consolidatedInfo.putIfAbsent(cloudlet, set);
                    //If the time is ok put it
                    set.add(info.getValue());
                }
            }

        }

        //Now for the value we can have a lot of options
        // I will choose for now the simplest, which is the value with the latest timestamp...
        Map<String,Tuple2> cloudletsAggregates = new HashMap<>();
        consolidatedInfo.entrySet().forEach(entry->{
            TreeSet<Tuple2> set = entry.getValue();
            if(!set.isEmpty()){
                cloudletsAggregates.put(entry.getKey(), set.first());
            }
        });


        Map<String,Set<Tuple2>> states = new HashMap<>();

        //APP LOGIC: Partitioning
        Map<String,String> cloudletsInZones = new HashMap<>();
        for (Map.Entry<String, Set<IProcess>> entry :Scheduler.getInstance().getVirtualFogTopolgy().getCloudletZones().entrySet()){
            entry.getValue().forEach(x->cloudletsInZones.put(x.getUuid(),entry.getKey()));
        }
        //Split values to zones
        for(Map.Entry<String, Tuple2> entry : cloudletsAggregates.entrySet()){
            String cloudlet = entry.getKey();
            Tuple2 value = entry.getValue();

            String zone = cloudletsInZones.get(cloudlet);
            Set<Tuple2> set = states.getOrDefault(zone, new HashSet<>());
            states.putIfAbsent(zone,set);
            set.add(value);

        }

        String aggregate;
        try { aggregate = (String) Scheduler.getInstance().getScenario().getProperty("aggregate","SUM"); } catch (MissingScenario missingScenario) { aggregate = "SUM"; }
        Map<String,TreeSet<Tuple2>> iots = new HashMap<>();
        states.values().forEach(objects->{
            for(Tuple2 tuple :objects) {
                Set<Tuple3> iotTuples = (Set<Tuple3>) tuple.getR2();
                for(Tuple3 iotTuple:iotTuples) {
                    String iot = (String) iotTuple.getR1();
                    long time = (long) iotTuple.getR2();
                    int value = (int) iotTuple.getR3();
                    TreeSet<Tuple2> set = iots.getOrDefault(iot, new TreeSet<>());
                    iots.putIfAbsent(iot,set);
                    set.add(new Tuple2(time,value));
                    //String time tuple.getR1();
                    //aggregatedState += (int)n;
                }
            }
        });


        Integer aggregatedState = 0;
        if(aggregate.equals("SUM")) {
            aggregatedState = iots.values().stream().mapToInt(e-> (int) e.first().getR2()).sum();
        }

        rep.setState(aggregatedState);
        return aggregatedState;

    }


    public Object calculateRealState() {
        String aggregate;
        try { aggregate = (String) Scheduler.getInstance().getScenario().getProperty("aggregate","SUM"); } catch (MissingScenario missingScenario) { aggregate = "SUM"; }

        if(aggregate.equals("SUM")) {

            return cloudModule.getlDevices().keySet().stream().map(e->getIots().get(e)).mapToInt( e->{
                try {
                    SSIoTModule module = (SSIoTModule) e.getModule(SSIoTModule.class);
                    return (int) module.getModel();
                } catch (MissingModule missingModule) {
                    return 0;
                }
            }).sum();
                    /**
            return getIots().values().stream().filter(e->e.isEnabled()).mapToInt(e -> {
                try {
                    SSIoTModule module = (SSIoTModule) e.getModule(SSIoTModule.class);
                    return (int) module.getModel();
                } catch (MissingModule missingModule) {
                    return 0;
                }
            }).sum();
                     **/
        }
        return -1;
    }



    public IProcess getCurrentLeader() {
       return currentLeader;
    }

    public IProcess getPreviousLeader() {
        return previousLeader;
    }

    public void setPreviousLeader(IProcess previousLeader) {
        this.previousLeader = previousLeader;
    }

    public Map<String,ZonesDescription> zonesDescriptionMap = new HashMap<>();
    public void addZonesDescription(String zone, ZonesDescription zonesDescription) {
        zonesDescriptionMap.put(zone,zonesDescription);
    }

    public Map<String, ZonesDescription> getZonesDescriptionMap() {
        return zonesDescriptionMap;
    }

    public Set<IProcess> getCurrentGuards() {
        return currentGuards;
    }

    private String linkToOtherCloudlets;

    public void setLinkToOtherCloudlets(String linkToOtherCloudlets) {
        this.linkToOtherCloudlets = linkToOtherCloudlets;
    }

    public String getLinkToOtherCloudlets() {
        return linkToOtherCloudlets;
    }

    public Object getRealState() {
    return calculateRealState();
    }

    private Map<String,Object> iotsModel =new HashMap<>();
    public void updateRealState(String uuid, Object model) {
        iotsModel.put(uuid,model);
    }
}
