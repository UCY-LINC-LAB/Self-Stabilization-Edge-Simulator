package dsslib.statistics;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import dsslib.App;
import dsslib.components.networking.SharedRegisterModule;
import dsslib.scheduler.Scheduler;
import dsslib.exceptions.MissingModule;
import dsslib.exceptions.MissingScenario;
import dsslib.logs.DirectoryService;
import dsslib.plots.PlotSnippets;
import dsslib.plots.ProcessExecutor;
import dsslib.selfstabilization.cloudlets.DataForSharedRegister;
import dsslib.scenarios.ScenarioDescriptionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Statistics {

    private static Logger logger = LoggerFactory.getLogger(Statistics.class);
    private static Statistics instance;

    private Map<String,Statistic> statistics = new HashMap<>();

    private String statsPath;
    private String plotsPath;
    private DirectoryService directoryService;

    private int every;
    private int plotsEvery;

    public Statistics() throws Exception {
        statistics.put("network",new NetworkStatistic("network"));
        statistics.put("aggregateState",new AggregateState("aggregateState"));
        statistics.put("sharedRegister",new SharedRegisterStatistic("sharedRegister"));
        statistics.put("selfStabilization",new SelfStabStatistic("selfStabilization"));

        //Register
        this.directoryService =DirectoryService.getInstance();
        this.statsPath = this.directoryService.getStatisticsPath();
        this.plotsPath = this.directoryService.getPlotsPath();


    }

    public Map<String,Object> getState(boolean gui) {
        Map<String,Object> ret = new HashMap<>();

        ret.putIfAbsent("events",getSelfStabStatistic().getEventsStats());

        if(!gui){
            ret.putIfAbsent("aggregateState",getAggregateState());
            ret.putIfAbsent("network",getNetworkStatistic().toJson());
        }

        return ret;
    }

    public void record(String id,Map<String,Object> props){
        Statistic statistic = statistics.get(id);
        if(statistic!=null) {
            try {
                long time = Scheduler.getInstance().getTime();
                int after = ScenarioDescriptionLoader.getScenarioDescription()
                        .getMode().getStatistics_after();
                if(time<after && !id.equals("selfStabilization"))
                    return;
                statistic.record(props);
            } catch (MissingScenario missingScenario) {
                missingScenario.printStackTrace();
            }
        }else{
            logger.info("Statistic "+id+" doesn't exist");
        }
    }

    public enum Event{
        NEW_LEADER,ELECTED_AS_GUARD, PROCESS_FAIL, PROCESS_ENABLED,
        START_INSTALL_PHASE_FOR_LEADER,
        START_INSTALL_PHASE_FOR_CLOUDLET,
        START_MULTICAST_PHASE_FOR_LEADER ,
        START_MULTICAST_PHASE_FOR_CLOUDLET,
        WRITE_DATA, NO_GUARD_ANYMORE,
        NO_LEADER_ANYMORE, START_PROPOSE_PHASE, COORDINATE_PROPOSE, FOLLOW_PROPOSE,
        LINK_FAILED, FOLLOW_INSTALL, FOLLOWING_MCAST_RND, REACHED_HEALTH_CHECK_THRESHOLD,

    }

    public NetworkStatistic getNetworkStatistic(){
        return (NetworkStatistic) statistics.get("network");
    }

    public AggregateState getAggregateState(){
        return (AggregateState) statistics.get("aggregateState");
    }

    public SharedRegisterStatistic getSharedRegisterStatistic(){
        return (SharedRegisterStatistic) statistics.get("sharedRegister");
    }

    public SelfStabStatistic getSelfStabStatistic(){
        return (SelfStabStatistic) statistics.get("selfStabilization");
    }


    public static synchronized Statistics getInstance(){
        if(instance == null) {
            try {
                instance = new Statistics();
            } catch (Exception e) {
                logger.error(e.getLocalizedMessage());
            }
        }
        return instance;
    }

    public void computeAndOutput(long time){
        int after = ScenarioDescriptionLoader.getScenarioDescription()
                .getMode().getStatistics_after();
        if(time<after)
            return;

        this.every = App.SCENARIO.getMode().getStatistics_every();
        this.plotsEvery = App.SCENARIO.getMode().getPlots_every();

        //Update state every step
        Object realState = Scheduler.getInstance().getVirtualFogTopolgy().getRealState();
        SharedRegisterModule shr = null;
        try {
            shr = (SharedRegisterModule) Scheduler.getInstance().getVirtualFogTopolgy().getSharedMemory().getModule(SharedRegisterModule.class);
        } catch (MissingModule missingModule) {
            missingModule.printStackTrace();
        }
        DataForSharedRegister data = (DataForSharedRegister) shr.getRecords().get("data");
        if(data!=null) {
            Object state = data.getState();

            if ((state != null) && (realState != null)) {
                //  System.out.println(time+"\t"+state+"\t"+realState);
                record("aggregateState", new HashMap<String, Object>() {{
                    put("state", state);
                    put("realState", realState);
                    put("rnd", data.getRnd());
                    put("process", data.getProcess());
                }});

            }
        }
        if(App.SCENARIO.getMode().isGui())
            return;

        if(time%every==0){
            logger.info("Computing statistics");

            if(ScenarioDescriptionLoader.getScenarioDescription().getMode().getStatistics().contains("network")) {

                String name = String.format("%06d", time);
                byte[] bytes;
                if(Scheduler.getInstance().getVirtualFogTopolgy().getProcesses().size()<100) {
                    name = String.format("%06d", time);
                    bytes = getNetworkStatistic().exchanges().getBytes();
                    write("network/packets", name + ".csv", bytes);
                    plot("network/packets", name + ".png", "heatmap.py", bytes, time, false);
                }

                bytes = getNetworkStatistic().timeMsgstoDataFrame().toString().getBytes();
                write("network", "msgs.csv", bytes);
                //plot("network", "count_msgs.png", "count_msgs.py", bytes, time, true);
                //plot("network", "size_msgs.png", "size_msgs.py", bytes, time, true);

            }

            if(ScenarioDescriptionLoader.getScenarioDescription().getMode().getStatistics().contains("aggregateState")) {
                String name = String.format("%06d", time);
                byte[] bytes = getAggregateState().getDataFrame().toString().getBytes();
                write("state", name+".csv", bytes);
                plot("state",name+".png","view_state.py",bytes,time,false);


            }
            if(ScenarioDescriptionLoader.getScenarioDescription().getMode().getStatistics().contains("selfStabilization")) {
                String str = getSelfStabStatistic().fetchPending();
                writeAppend("./", "events.csv", str);

            }
        }
    }

    public void plot(String dir,String name,String script, byte[] bytes,long time,boolean singleThread){
        if(plotsEvery==0)
            return;
        if(time%plotsEvery!=0)
            return;

        File folder = Paths.get(plotsPath,dir).toFile();
        folder.mkdirs();

        String path =  Paths.get(folder.getAbsolutePath(),name).toAbsolutePath().toString();
        ProcessExecutor plt =  new ProcessExecutor()
                .command("python3", PlotSnippets.getPath(script),
                        "--export",path)
                .setInputData(bytes);

        if(singleThread)
            App.SINGLE_EXECUTOR.submit(plt);
        else
            App.EXECUTORS.submit(plt);

    }
    public void writeSelfStabStatistics() throws IOException {
        File file = Paths.get(statsPath,"events.json").toFile();
        Object ob = getSelfStabStatistic().getEventsStats();

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,true);
        mapper.writerWithDefaultPrettyPrinter().writeValue(new FileWriter(file),ob);
    }

    public void writeAppend(String dir,String name,String data){
        File folder = Paths.get(statsPath,dir).toFile();
        folder.mkdirs();
        File file = Paths.get(folder.getAbsolutePath(),name).toFile();

        try {
            directoryService.append(file,data);
        } catch (IOException e) {
            logger.error("Couldn't write to file: "+e.getLocalizedMessage());
        }
    }
    public void write(String dir,String name,byte[] bytes){
        File folder = Paths.get(statsPath,dir).toFile();
        folder.mkdirs();
        File file = Paths.get(folder.getAbsolutePath(),name).toFile();

        try {
            directoryService.write(file,bytes);
        } catch (IOException e) {
            logger.error("Couldn't write to file: "+e.getLocalizedMessage());
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Statistics:\n");
        sb.append("\tOutgoing Messages:\n\t\t"+(getNetworkStatistic().getNewSendMessages().entrySet().stream()
                .sorted(new NetworkStatistic.FromToEntry.OrderByName())
                .map(e->e.toString()).collect(Collectors.joining("\n\t\t"))));

        return sb.toString();
    }
}
