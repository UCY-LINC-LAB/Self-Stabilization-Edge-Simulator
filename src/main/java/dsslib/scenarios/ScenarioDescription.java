package dsslib.scenarios;

import dsslib.process.IProcess;
import dsslib.components.IModule;
import dsslib.components.selfstabilization.SSCloudModule;
import dsslib.components.selfstabilization.SSIoTModule;
import dsslib.logs.DirectoryService;
import dsslib.utilities.UtilRand;
import dsslib.scenarios.description.*;
import dsslib.scenarios.environment.EnvironmentEvent;
import dsslib.scenarios.environment.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class ScenarioDescription {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioDescription.class);
    private String root="output";
    private String name="name";
    private long random_seed = 0;
    private SimulationMode mode;
    private ProcessesDescription processes;
    private NetworkDescription network;
    private EventsDescription events;
    private Map<String,Object> properties;

    public static Logger getLogger() {
        return logger;
    }

    public NetworkDescription getNetwork() {
        return network;
    }

    public void setNetwork(NetworkDescription network) {
        this.network = network;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public String getRoot() {
        return root;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    public ProcessesDescription getProcesses() {
        return processes;
    }

    public void setProcesses(ProcessesDescription processes) {
        this.processes = processes;
    }

    public long getRandom_seed() {
        return random_seed;
    }

    public EventsDescription getEvents() {
        return events;
    }

    public SimulationMode getMode() {
        return mode;
    }

    public void setMode(SimulationMode mode) {
        this.mode = mode;
    }

    public void setEvents(EventsDescription events) {
        this.events = events;
    }

    public void setRandom_seed(long random_seed) {
        this.random_seed = random_seed;
    }

    @Override
    public String toString() {
        return "ScenarioDescription{" +
                "random_seed=" + random_seed +
                ", processes=" + processes +
                ", events=" + events +
                '}';
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Scenario build() throws Exception {
        DirectoryService.init();
        Scenario scenario = new Scenario();
        scenario.setRandom(new Random(this.getRandom_seed()));
        scenario.setMode(this.mode);
        scenario.setProperties(properties);
        VirtualFogTopology topology = new VirtualFogTopology();

        CloudDescription cloudDescription = processes.getCloud();
        IProcess cloud  =  topology.createCloud(cloudDescription.getSpeed());
        registerModules(cloud,cloudDescription.getModules());
        topology.setCloudModule((SSCloudModule) cloud.getModule(SSCloudModule.class));

        topology.registerProcess(cloud);
        topology.registerToSharedMemory(cloud);

        NetworkDescription networkDescription = this.getNetwork();

        //Cloudlets
        CloudletsDescription cloudletsDescription = processes.getCloudlets();
        topology.setLinkToOtherCloudlets(processes.getCloudlets().getLink_to_other_cloudlets());
        for(ZonesDescription zonesDescription: processes.getCloudlets().getZones()){

            String linksToCloudName = zonesDescription.getLinks().get("cloud");
            NetworkProfile networkProfileToCloud = networkDescription.get(linksToCloudName);
            LinkSpeedDescription speedToCloud = networkProfileToCloud.getSpeed();

            for(int i=0;i<zonesDescription.getCount();i++){
                IProcess cloudlet  =  topology.createCloudlet(cloudletsDescription.getSpeed(),zonesDescription.getZone());

                registerModules(cloudlet,cloudletsDescription.getModules());

                topology.registerProcess(cloudlet);
                topology.registerToSharedMemory(cloudlet);
                //Link to cloud
                topology.link(cloudlet.getUuid()
                        ,"cloud",speedToCloud
                        ,networkProfileToCloud.getUpstreamBandwidth()
                        ,networkProfileToCloud.getDownstreamBandwidth());
            }
            //Update Links among Cloudlets in the same zone
            IProcess[] allCloudlets = topology.getCloudletZones().get(zonesDescription.getZone()).toArray(new IProcess[]{});
            String linksToCloudletsNames = zonesDescription.getLinks().get("cloudlets");
            NetworkProfile networkProfileToCloudlets = networkDescription.get(linksToCloudletsNames);
            LinkSpeedDescription speedToCloudlets = networkProfileToCloudlets.getSpeed();
            for(int i=0;i<allCloudlets.length-1;i++){
                for(int j=i+1;j<allCloudlets.length;j++){
                    //Link to cloudlet
                    topology.link(allCloudlets[i].getUuid()
                            ,allCloudlets[j].getUuid()
                            ,speedToCloudlets
                            ,networkProfileToCloudlets.getUpstreamBandwidth()
                            ,networkProfileToCloudlets.getDownstreamBandwidth());

                }

            }
        }
        //Update Lings for cloudlets in different zones
        String[] zones = topology.getCloudletZones().keySet().toArray(new String[]{});
        for(int i=0;i<zones.length-1;i++){
            for(int j=i+1;j<zones.length;j++){
                String zone_a = zones[i];
                String zone_b = zones[j];
                String linksDescriptionName = topology.getLinkToOtherCloudlets();
                NetworkProfile networkProfileToCloudlets = networkDescription.get(linksDescriptionName);
                LinkSpeedDescription speedToCloudlets = networkProfileToCloudlets.getSpeed();

                IProcess[] cloudlets_a = topology.getCloudletZones().get(zone_a).toArray(new IProcess[]{});
                IProcess[] cloudlets_b = topology.getCloudletZones().get(zone_b).toArray(new IProcess[]{});

                for(int a=0;a<cloudlets_a.length;a++) {
                    for (int b = 0; b < cloudlets_b.length; b++) {

                        //Link to cloudlet
                        topology.link(cloudlets_a[a].getUuid()
                                ,cloudlets_b[b].getUuid()
                                ,speedToCloudlets
                                ,networkProfileToCloudlets.getUpstreamBandwidth()
                                ,networkProfileToCloudlets.getDownstreamBandwidth());
                    }
                }
            }
        }







        //IoTs
        IotsDescription iotsDescription = processes.getIots();
        for(ZonesDescription zonesDescription: iotsDescription.getZones()){
            String linksToCloudName = zonesDescription.getLinks().get("cloud");
            NetworkProfile networkProfileToCloud = networkDescription.get(linksToCloudName);

            Set<IProcess> cloudlets = null;

            if(topology.getCloudletZones().containsKey(zonesDescription.getZone())) {
                    cloudlets = topology.getCloudletZones().get(zonesDescription.getZone());
            }

            for(int i=0;i<zonesDescription.getCount();i++){
                IProcess iot  =  topology.createIoT(iotsDescription.getSpeed(),zonesDescription.getZone());
                registerModules(iot,iotsDescription.getModules());
                topology.registerProcess(iot);
                SSIoTModule m = (SSIoTModule) iot.getModule(SSIoTModule.class);
                iotUpdate = m.getModelUpdatePeriodicity();
                //Link to cloud
                topology.link(iot.getUuid()
                        ,"cloud",networkProfileToCloud.getSpeed()
                        ,networkProfileToCloud.getUpstreamBandwidth()
                        ,networkProfileToCloud.getDownstreamBandwidth());

                //Link to cloudlets if in the same zone
                if(topology.getCloudletZones().containsKey(zonesDescription.getZone())){
                    String linksToCloudletsNames = zonesDescription.getLinks().get("cloudlets");
                    NetworkProfile networkProfileToCloudlets = networkDescription.get(linksToCloudletsNames);

                    for (IProcess cloudlet : cloudlets) {
                        //Link to cloudlet
                        topology.link(iot.getUuid()
                                ,cloudlet.getUuid(),networkProfileToCloudlets.getSpeed()
                                ,networkProfileToCloudlets.getUpstreamBandwidth()
                                ,networkProfileToCloudlets.getDownstreamBandwidth());

                        //As we have link, then cloudlet must be responsible
                        // Also update iot cloudletList
                        topology.addIoTtoCloudletResponsibility(cloudlet.getUuid(),iot.getUuid());

                    }
                }
            }

        }


        //Adding Events
        Random random = scenario.getRandom();
        if(events!=null) {
            for (Map<String, Object> e : events) {

                Integer at = (Integer) e.get("at");
                if (at == null) {
                    throw new Exception("Key: at, expects integer");
                }
                String type = (String) e.get("type");
                if (type == null)
                    throw new Exception("Key: type, is missing");


                if (type.equalsIgnoreCase("FAIL_RANDOM_CLOUDLETS")) {
                    Integer count = (Integer) e.getOrDefault("count", 1);
                    Integer comeback = (Integer) e.get("comeback");

                    List<IProcess> rndCloudlets = UtilRand.getRandom(random, new LinkedList<>(topology.getCloudlets().values()), count);
                    rndCloudlets.forEach(proc -> {
                        EnvironmentEvent event = new EnvironmentEvent(at, EventType.FAIL_CLOUDLET, proc.getUuid());
                        scenario.addEvent(event);
                        logger.info("Registered event: " + event);
                        if (comeback != null) {
                            event = new EnvironmentEvent(at + comeback, EventType.ENABLE_PROCESS,
                                    proc.getUuid());
                            scenario.addEvent(event);
                            logger.info("Registered event: " + event);
                        }
                    });
                } else if (type.equalsIgnoreCase("FAIL_LEADER")) {
                    EnvironmentEvent event = new EnvironmentEvent(at, EventType.FAIL_LEADER, "");
                    scenario.addEvent(event);
                    logger.info("Registered event: " + event);

                } else if (type.equalsIgnoreCase("ALL_CLOUDLETS_BELIEVE_THEY_ARE_LEADER")) {
                    EnvironmentEvent event = new EnvironmentEvent(at, EventType.ALL_CLOUDLETS_BELIEVE_THEY_ARE_LEADER, "");
                    int rounds = (int) e.getOrDefault("rounds", 1);
                    event.addProperty("rounds", rounds);
                    scenario.addEvent(event);
                    logger.info("Registered event: " + event);

                } else if (type.equalsIgnoreCase("ENABLE_PREVIOUS_LEADER")) {
                    EnvironmentEvent event = new EnvironmentEvent(at, EventType.ENABLE_PREVIOUS_LEADER, "");
                    scenario.addEvent(event);
                    logger.info("Registered event: " + event);

                } else if (type.equalsIgnoreCase("FAIL_CLOUDLET")) {
                    String name = (String) e.get("name");
                    if (name != null) {
                        IProcess iProcess = topology.getCloudlets().get(name);
                        if (iProcess != null) {
                            EnvironmentEvent event = new EnvironmentEvent(at, EventType.FAIL_CLOUDLET, iProcess.getUuid());
                            scenario.addEvent(event);
                            logger.info("Registered event: " + event);
                        }

                    }
                } else if (type.equalsIgnoreCase("ENABLE_CLOUDLET")) {
                    String name = (String) e.get("name");
                    if (name != null) {
                        IProcess iProcess = topology.getCloudlets().get(name);
                        if (iProcess != null) {
                            EnvironmentEvent event = new EnvironmentEvent(at, EventType.ENABLE_PROCESS, iProcess.getUuid());
                            scenario.addEvent(event);
                            logger.info("Registered event: " + event);
                        }

                    }

                } else if (type.equalsIgnoreCase("FAIL_PROCESS")) {
                    String name = (String) e.get("name");
                    if (name != null) {
                        EnvironmentEvent event = new EnvironmentEvent(at, EventType.FAIL_PROCESS, name);
                        scenario.addEvent(event);
                        logger.info("Registered event: " + event);

                    }
                } else if (type.equalsIgnoreCase("FAIL_LINK")) {
                    String p1 = (String) e.get("p1");
                    String p2 = (String) e.get("p2");
                    EnvironmentEvent event = new EnvironmentEvent(at, EventType.FAIL_LINK, "");
                    event.addProperty("p1", p1);
                    event.addProperty("p2", p2);
                    scenario.addEvent(event);
                    logger.info("Registered event: " + event);


                } else if (type.equalsIgnoreCase("FAIL_RANDOM_CLOUDLETS_ONLY")) {
                    Integer count = (Integer) e.getOrDefault("count", 1);
                    Integer comeback = (Integer) e.get("comeback");
                    EnvironmentEvent event = new EnvironmentEvent(at, EventType.FAIL_CLOUDLETS_ONLY, "");
                    event.addProperty("count", count);
                    if (comeback != null)
                        event.addProperty("comeback", comeback);
                    scenario.addEvent(event);
                    logger.info("Registered event: " + event);
                } else if (type.equalsIgnoreCase("ENABLE_ALL_RANDOM")) {
                    Integer from = (Integer) e.getOrDefault("from", 0);
                    Integer to = (Integer) e.getOrDefault("to", 1000);
                    EnvironmentEvent event = new EnvironmentEvent(at, EventType.ENABLE_ALL_RANDOM, "");
                    event.addProperty("from", from);
                    event.addProperty("to", to);
                    scenario.addEvent(event);
                    logger.info("Registered event: " + event);
                } else if (type.equalsIgnoreCase("FAIL_GUARDS")) {
                    Integer count = (Integer) e.getOrDefault("count", 1);
                    Integer comeback = (Integer) e.get("comeback");
                    EnvironmentEvent event = new EnvironmentEvent(at, EventType.FAIL_GUARDS, "");
                    event.addProperty("count", count);
                    if (comeback != null)
                        event.addProperty("comeback", comeback);
                    scenario.addEvent(event);
                    logger.info("Registered event: " + event);
                } else if (type.equalsIgnoreCase("FAIL_LINKS_IOT_TO_CLOUDLET")) {
                    Integer from = (Integer) e.getOrDefault("from", 0);
                    Integer to = (Integer) e.getOrDefault("to", 1000);
                    Integer count = (Integer) e.getOrDefault("count", 1);
                    List<String> regions = (List<String>) e.getOrDefault("regions", new LinkedList<>());
                    EnvironmentEvent event = new EnvironmentEvent(at, EventType.FAIL_LINKS_IOT_TO_CLOUDLET, "");
                    event.addProperty("count", count);
                    event.addProperty("from", from);
                    event.addProperty("to", to);
                    event.addProperty("regions", regions);
                    scenario.addEvent(event);
                    logger.info("Registered event: " + event);


                } else if (type.equalsIgnoreCase("FAIL_IOTS_ONLY")) {
                    Integer count = (Integer) e.getOrDefault("count", 1);
                    Integer comeback = (Integer) e.get("comeback");
                    EnvironmentEvent event = new EnvironmentEvent(at, EventType.FAIL_IOTS_ONLY, "");
                    event.addProperty("count", count);
                    if (comeback != null) event.addProperty("comeback", comeback);
                    scenario.addEvent(event);
                    logger.info("Registered event: " + event);
                }
            }
        }

        int recordSize = (int) properties.getOrDefault("recordSize",32);
        int records = (int) properties.getOrDefault("records",16);
        aggregateSize = recordSize*records;
        scenario.setTopology(topology);





        return scenario;
    }
    public void registerModules(IProcess process, List<ModulesDescription> modules) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        for(ModulesDescription modulesDescription : modules){
            //String module = modulesDescription.getModuleRealName();
            String impl = modulesDescription.getImplementation();
            Map<String,Object> params = modulesDescription.getParams();
            //Class<?> clazzModule = getClass().getClassLoader().loadClass(module);
            Class<?> clazzImpl = getClass().getClassLoader().loadClass(impl);
            process.registerComponent(loadModule(clazzImpl,process,params));
            //System.out.println(clazzModule);
            //System.out.println(clazzImpl);
        }
    }
    public <T> IModule loadModule(Class<? extends T> clazzImpl, IProcess process, Map<String,Object> props) throws IllegalAccessException,
            InstantiationException, NoSuchMethodException, InvocationTargetException {
        Class[] cArgs;
        if(props.size()==0){
            cArgs = new Class[]{IProcess.class};
            return (IModule) clazzImpl.getDeclaredConstructor(cArgs).newInstance(process);
        }else{
            cArgs = new Class[]{IProcess.class,Map.class};
            return (IModule) clazzImpl.getDeclaredConstructor(cArgs).newInstance(process,props);
        }


    }

    private int aggregateSize;
    private int iotUpdate;

    public int getIotUpdate() {
        return iotUpdate;
    }

    public int getAggregateSize() {
        return aggregateSize;
    }

    public void setAggregateSize(int aggregateSize) {
        this.aggregateSize = aggregateSize;
    }
}
