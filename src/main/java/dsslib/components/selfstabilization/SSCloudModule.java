package dsslib.components.selfstabilization;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.components.healthcheck.HealthCheckModule;
import dsslib.components.networking.NetworkModule;
import dsslib.components.networking.OperationPacket;
import dsslib.components.networking.SharedRegisterModule;
import dsslib.components.timers.PeriodicTimerModule;
import dsslib.scheduler.Scheduler;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.exceptions.MissingModule;
import dsslib.exceptions.MissingScenario;
import dsslib.exceptions.SimulationException;
import dsslib.selfstabilization.IoTModelTime;
import dsslib.selfstabilization.RegisterInfoModel;
import dsslib.selfstabilization.SeqIdPair;

import java.util.*;

public abstract class SSCloudModule extends AbstractModule {

    protected final int periodicity;

    protected  int checkPeriodForCloudlets =300;
    protected  int checkPeriodForLeader =150;

    /** A set of new cloudlets in the same form as cloudCloudletSet odels; **/
    protected Map<String, Integer > newCloudlet = new HashMap<>();

    /** a set (bounded by deviceSetSize) of new IoT devices in the same form as deviceSet **/
    protected Map<String,IoTModelTime> newIot = new HashMap<>();

    /** The leadership sequence number **/
    protected int sequence;


    /** Read from shared Register**/
    protected Map<String,IoTModelTime> lDevices = new HashMap<>();
    protected Map<String, Integer> lCloudlets = new HashMap<>();
    protected SeqIdPair lLeader = new SeqIdPair(-1,"");
    protected Set<String> lguards = new LinkedHashSet<>();

    /** Read from shared Register **/
    protected RegisterInfoModel lInfo;

    public abstract EventHandler<MsgFromIoT> onMsgFromIoT();

    public abstract EventHandler<MsgFromCloudlet> onMsgFromCloudlet();

    public abstract EventHandler<NetworkModule.NewMessageReceived> onNewMessageReceived();

    public abstract EventHandler<PeriodicTimerModule.PeriodExpired> onPeriodExpired();

    public abstract EventHandler<HealthCheckModule.HostIsUp> onHostIsUp();

    public abstract EventHandler<HealthCheckModule.HostIsDown> onHostIsDown();

    public abstract EventHandler<HealthCheckModule.HostIsUpButLate> onHostIsUpButLate();

    /**
     * When we have an answer from the shared register
     * @return
     */
    public abstract EventHandler<SharedRegisterModule.ReadResponse> onReadResponse();


    /** The following are controlled from the Factory, since we haven't implemented yet the necessary modules,
     *  for example, leader election, suspeted Iot and suspected Cloudlet.
     * **/

    /**
     * the set of suspected to be faulty IoT devices
     * @param iots
     * @return
     */
    public abstract Set<String> suspectedIot(Set<String> iots);

    /**
     * the set of suspected to be faulty cloudlets
     * @param cloudlets
     * @return
     */
    public abstract Set<String> suspectedCloudlet(Set<String> cloudlets);


    /**
     * Elect a leader from the given set
     * @param cloudlets
     * @return
     */
    public abstract String electLeader(Set<String> cloudlets) throws SimulationException, MissingScenario;

    /**
     * the set of guards  from the set
     * @param cloudlets
     * @return
     */
    public abstract Set<String> selectGuards(Set<String> cloudlets,Set<String> guards) throws MissingScenario;



    public static final class MsgFromCloudlet extends Event{
        private final String from;
        private final OperationPacket pckt;

        public MsgFromCloudlet(String from, OperationPacket pckt) {
            this.from = from;
            this.pckt = pckt;
        }

        public OperationPacket getPckt() {
            return pckt;
        }

        public String getFrom() {
            return from;
        }
    }

    public static final class MsgFromIoT extends Event {
        private final String from;
        private final OperationPacket pckt;

        public MsgFromIoT(String from, OperationPacket pckt){
            this.from = from;
            this.pckt = pckt;
        }

        public OperationPacket getPckt() {
            return pckt;
        }

        public String getFrom() {
            return from;
        }
    }

    public SSCloudModule(IProcess process, Map<String,Object> props) throws MissingModule {
        super(process,props);
        this.periodicity = (int) props.get("periodicity");
        this.checkPeriodForCloudlets = (int) props.get("checkPeriodForCloudlets");
        this.checkPeriodForLeader = (int) props.get("checkPeriodForLeader");

        //Networking
        require(NetworkModule.class);
        subscribe(onNewMessageReceived(), NetworkModule.NewMessageReceived.class);

        //For Healthcheck module
        require(HealthCheckModule.class);
        subscribe(onHostIsDown(), HealthCheckModule.HostIsDown.class);
        subscribe(onHostIsUp(), HealthCheckModule.HostIsUp.class);
        subscribe(onHostIsUpButLate(), HealthCheckModule.HostIsUpButLate.class);


        //Shared Register
        require(SharedRegisterModule.class);
        subscribe(onReadResponse(),SharedRegisterModule.ReadResponse.class);

        //Periodic Timer
        require(PeriodicTimerModule.class);
        subscribe(onPeriodExpired(), PeriodicTimerModule.PeriodExpired.class);

        subscribe(onMsgFromIoT(),MsgFromIoT.class);
        subscribe(onMsgFromCloudlet(),MsgFromCloudlet.class);
    }

    public void setCheckPeriodForCloudlets(int checkPeriodForCloudlets) {
        this.checkPeriodForCloudlets = checkPeriodForCloudlets;
    }

    public void setCheckPeriodForLeader(int checkPeriodForLeader) {
        this.checkPeriodForLeader = checkPeriodForLeader;
    }

    /**
     * Return all the available cloudlets
     * @return
     */
    public Set<String> C(){
        return new HashSet<>(Scheduler.getInstance().getVirtualFogTopolgy().getCloudlets().keySet());
    }

    public Map<String, IoTModelTime> getlDevices() {
        return lDevices;
    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> state = new HashMap<>();
        state.put("periodicity",periodicity);
        state.put("checkPeriodForCloudlets",checkPeriodForCloudlets);
        state.put("checkPeriodForLeader",checkPeriodForLeader);
        state.put("newCloudlet",newCloudlet);
        state.put("newIot",newIot);
        state.put("sequence",sequence);
        state.put("lDevices",lDevices);
        state.put("lCloudlets",lCloudlets);
        return state;
    }
}
