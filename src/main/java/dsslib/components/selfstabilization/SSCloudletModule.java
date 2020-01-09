package dsslib.components.selfstabilization;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.components.healthcheck.HealthCheckModule;
import dsslib.components.networking.NetworkModule;
import dsslib.components.networking.SharedRegisterModule;
import dsslib.components.timers.PeriodicTimerModule;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.exceptions.MissingModule;
import dsslib.selfstabilization.*;
import dsslib.utilities.Tuple2;
import dsslib.utilities.Tuple3;

import java.util.*;
import java.util.stream.Collectors;

public abstract class SSCloudletModule extends AbstractModule {

    /******************************************** LOCAL STATE ************************************/
    /**
     * A set (bounded by deviceSetSize) of IoT devices and their most recently
     * received models
     */
    protected Map<String, IoTModelTime> deviceSet = new HashMap<>();

    /** A set of data structures encoding aggregated sensory info **/
    protected Map<String, Tuple2> aggregateInfo = new HashMap<>();

    /** A positive integer used for ordering message sent to the leader and guards **/
    protected long msgc;

    /** A positive integer used for ordering messages sent to IoT devices **/
    protected long msgtoiot;

    /** A set of (id,seq) pairs that stores the highest message sequence received by cloudlet id **/
    protected Map<String,Long> MSGc = new HashMap<>();

    /** A set of (id,seq) pairs that stores the highest message sequence received by IoT id **/
    protected Map<String,Long> MSGSEQ = new HashMap<>();


    /** Fro mshared register **/
    protected RegisterInfoModel lInfo;

    /******************************************* INTERFACE ***************************************/



    /**
     * Return the aggregated sensory information
     * @param deviceSet
     * @return
     */
    public abstract Set<Tuple3> aggregate(Map<String,IoTModelTime> deviceSet);


    /**
     * For a given IoT device pk and cloudletSet, this function returns the cloudlet list that pk
     * should use (prioritized in an descending order);
     * @param iot
     * @return
     */
    public abstract PriorityQueue<CloudletWithPriority> cloudletList(String iot);

    /**
     * projection of the IoTs that are within this cloudlet myIoT;
     * @param localDevices
     * @param lCloudlets
     * @return
     */
    public abstract Set<String> myIoT(Set<String> localDevices, Map<String, Integer> lCloudlets);


    /** Cleans all local variables and all communication links. Sends a request to the cloud for reregistration * */
    public abstract void cloudletInit();

    /**********************************************************************************************/



    /** The Id of the current cloudlet leader **/
    protected SeqIdPair lLeader = new SeqIdPair(-1,"");
    /** The set of the ids of the cloudlet guards **/
    protected Set<String> lGuards = new LinkedHashSet<>();
    Map<String,Integer> lCloudlets =new HashMap<>();
    Map<String, IoTModelTime> lDevices =new HashMap<>();

    protected final int periodicity;
    protected String uuidForPeriodicTimer;








    /**
     * When the period is timeout
     * @return
     */
    public abstract EventHandler<PeriodicTimerModule.PeriodExpired> onPeriodTimeout();


    /**
     * When we have an answer from the shared register
     * @return
     */
    public abstract EventHandler<SharedRegisterModule.ReadResponse> onReadResponse();




    public abstract EventHandler<SSCloudletLeaderModule.Fetch> onFetch();
    public abstract EventHandler<MsgFromIoT> onMsgFromIoT();
    public abstract EventHandler<MsgFromCloudlet> onMsgFromCloudlet();
    public abstract EventHandler<NetworkModule.NewMessageReceived> onNewMessageReceived();

    public static final class MsgFromIoT extends Event {
        private final String from;
        private final IoTMsgToCloudlet ioTMsgToCloudlet;

        public MsgFromIoT(String from, IoTMsgToCloudlet ioTMsgToCloudlet) {
            this.from = from;
            this.ioTMsgToCloudlet = ioTMsgToCloudlet;
        }

        public IoTMsgToCloudlet getIoTMsgToCloudlet() {
            return ioTMsgToCloudlet;
        }

        public String getFrom() {
            return from;
        }
    }
    public static final class MsgFromCloudlet extends Event {
        private final String from;
        private final long msgSeq;
        private final Object aggregateInfo;

        public MsgFromCloudlet(String from,long msgSeq, Object aggregateInfo) {
            this.from = from;
            this.msgSeq = msgSeq;
            this.aggregateInfo = aggregateInfo;
        }

        public long getMsgSeq() {
            return msgSeq;
        }

        public Object getAggregateInfo() {
            return aggregateInfo;
        }

        public String getFrom() {
            return from;
        }
    }



    /**
     * Returns the set of suspected to be faulty cloudlets
     * @param cloudlets
     * @return
     */
    public abstract Set<String> suspectedCloudlet(Set<String> cloudlets);

    /**
     * return true if the cloudlet id is suspected to be faulty
     * @param cloudlet
     * @return
     */
    public abstract boolean checkCloudlet(String cloudlet);


    public SSCloudletModule(IProcess process,Map<String,Object> props) throws MissingModule {
        super(process,props);
        this.periodicity = (int) props.get("periodicity");
        subscribe(onMsgFromCloudlet(),MsgFromCloudlet.class);
        subscribe(onMsgFromIoT(),MsgFromIoT.class);

        require(NetworkModule.class);
        subscribe(onNewMessageReceived(),NetworkModule.NewMessageReceived.class);

        require(HealthCheckModule.class);
        require(PeriodicTimerModule.class);
        subscribe(onPeriodTimeout(), PeriodicTimerModule.PeriodExpired.class);

        require(SharedRegisterModule.class);
        subscribe(onReadResponse(), SharedRegisterModule.ReadResponse.class);


        subscribe(onFetch(), SSCloudletLeaderModule.Fetch.class);
    }
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\t\taggregateInfo:"+aggregateInfo);
        sb.append("\n\t\t\tlLeader:"+lLeader);
        sb.append("\n\t\t\tdeviceSet:\n\t\t\t\t"+deviceSet.values().stream().map(e->e.toString()).collect(Collectors.joining("\n\t\t\t\t")));
        return sb.toString();
    }
}
