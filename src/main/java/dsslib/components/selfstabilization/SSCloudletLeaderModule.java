package dsslib.components.selfstabilization;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.components.networking.NetworkModule;
import dsslib.components.networking.SharedRegisterModule;
import dsslib.components.timers.PeriodicTimerModule;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.exceptions.MissingModule;
import dsslib.selfstabilization.SeqIdPair;
import dsslib.selfstabilization.cloudlets.Replica;
import dsslib.utilities.Tuple2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Execution should happen only when the respected process is a leader or a guard
 */
public abstract class SSCloudletLeaderModule extends AbstractModule {

    //Used to check if I should execute this module
    protected boolean amIleader;
    protected boolean amIGuard;

    protected final int periodicity;
    protected String uuidForPeriodicTimer;


    protected Map<String,Integer> healthCheck = new HashMap<>();
    protected Integer healthCheckThreshold;
    protected int maxBeat=0;
    protected String maxBeatFrom="";

    //For shared Register
    protected SeqIdPair lLeader = new SeqIdPair(-1,"");
    protected Set<String> lGuards = new HashSet<>();
    //protected Map<String, IoTModelTime> lDevices;
    protected Map<String, Integer> lCloudlets;

    /******************************************* Variables ******************************************/

    /** An array of the state machine's replica. **/
    protected Map<String, Replica> rep = new HashMap<>();


    /** A local variable FD stores the failureDetector() output. They key is pid and value crdID  => <pid,crdID> **/
    protected Set<String> FD = new HashSet<>();

    /** Stores the id of the local leader **/
    protected String myLeader = "";

    /****************************************** Interfaces ******************************************/

    /** returns a vector of processor pairs <pid, crdID> **/
    public abstract Object synchState(Replica replica);
    public abstract Object apply(Replica state, Map<String,Map<String, Tuple2>> msg, Set<String> active);
    public abstract Map<String,Map<String,Tuple2>> synchMsgs(Replica replicas);
    public abstract Set<String> failureDetector();


    /******************* MACROS***********/
    public abstract boolean roundProceedReady();
    public abstract boolean roundReadyToFollow();
    public abstract void coordinateMcastRnd();
    public abstract void coordinateInstall();
    public abstract void coordinatePropose();
    public abstract void followMcastRnd();
    public abstract void followInstall();
    public abstract void followPropose();

    /********* EVENTS**********/
    public abstract EventHandler<UpdateLeadership> onUpdateLeaderShip();
    public abstract EventHandler<ResetLeaderShip> onResetLeaderShip();
    public abstract EventHandler<NetworkModule.NewMessageReceived> onNewMessageReceived();

    public abstract EventHandler<FetchResponse> onFetchResponse();


    /**
     * When we have an answer from the shared register
     * @return
     */
    public abstract EventHandler<SharedRegisterModule.ReadResponse> onReadResponse();
    public abstract EventHandler<SharedRegisterModule.WriteResponse> onWriteResponse();


    /**
     * When the period is timeout
     * @return
     */
    public abstract EventHandler<PeriodicTimerModule.PeriodExpired> onPeriodTimeout();

    public  enum Type{
        LEADER,GUARD
    }
    public static final class UpdateLeadership extends Event {
        public UpdateLeadership(Type type, boolean value) {
            this.type = type;
            this.value = value;
        }
        private final Type type;
        private final boolean value;

        public Type getType() {
            return type;
        }

        public boolean getValue() {
            return value;
        }
    }
    public static final class ResetLeaderShip extends Event { }

    public static final class Fetch extends Event{
        private final String whoCalled;

        public Fetch(String whoCalled) {
            this.whoCalled = whoCalled;
        }

        public String getWhoCalled() {
            return whoCalled;
        }
    }
    public static final class FetchResponse extends Event{
        private final String whoCalled;
        private final Map<String, Tuple2> aggregate;

        public FetchResponse(String whoCalled, Map<String, Tuple2> aggregate) {
            this.whoCalled = whoCalled;
            this.aggregate = aggregate;
        }

        public Map<String, Tuple2> getAggregate() {
            return aggregate;
        }

        public String getWhoCalled() {
            return whoCalled;
        }
    }


    public SSCloudletLeaderModule(IProcess process, Map<String, Object> props) throws MissingModule {
        super(process, props);
        this.periodicity = (int) props.get("periodicity");
        this.healthCheckThreshold = (int) props.getOrDefault("healthCheckThreshold",4);
        subscribe(onUpdateLeaderShip(),UpdateLeadership.class);
        subscribe(onResetLeaderShip(),ResetLeaderShip.class);

        require(NetworkModule.class);
        subscribe(onNewMessageReceived(), NetworkModule.NewMessageReceived.class);

        require(PeriodicTimerModule.class);
        subscribe(onPeriodTimeout(), PeriodicTimerModule.PeriodExpired.class);

        require(SharedRegisterModule.class);
        subscribe(onReadResponse(), SharedRegisterModule.ReadResponse.class);
        subscribe(onWriteResponse(), SharedRegisterModule.WriteResponse.class);



        subscribe(onFetchResponse(),FetchResponse.class);

    }

    public int getMaxBeat() {
        return maxBeat;
    }
}
