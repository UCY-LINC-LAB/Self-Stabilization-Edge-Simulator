package dsslib.components.selfstabilization;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.components.networking.NetworkModule;
import dsslib.components.timers.PeriodicTimerModule;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.exceptions.MissingModule;
import dsslib.selfstabilization.CloudletWithPriority;

import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

public abstract class SSIoTModule extends AbstractModule {


    protected int modelUpdatePeriodicity=500;

    protected final int periodicity;
    /** In the paper this is referred as LIMIT **/
    protected final int timeout;


    /************************************* LOCAL STATE ***************************************/

    /** A data structure encoding the recent sensory readings **/
    protected Object model = 0;

    /** Most recent model received by a cloudlet **/
    protected Object cloudletModel = -1;

    /** A list of dissemination points **/
    protected PriorityQueue<CloudletWithPriority> cloudletList;

    /** time of the last update reception from a cloudlet **/
    protected long lastUpdate=0;

    /** A positive integer used as a sequence number for messages **/
    protected long msgseq;

    /** A set of (id,seq) pairs that stores the highest message sequence received by cloudlet id**/
    protected Map<String,Long> MSG = new HashMap<>();


    /************************************* INTERFACES ***************************************/

    /**
     * Receives the device and cloudlet models as well as lastUpdate and returns true if,
     * and only if, the cloudlet model requires an update due to change in sensory input or timeout handled
     * by failure detection mechanisms;
     * @return
     */
    public abstract boolean update();

    /**
     * takes the lastUpdate and compare it to the local clock to see whether it has been too
     * long since any message has arrived from any cloudlet;
     */
    public abstract boolean timingFailure();

    public abstract void iotInit();


    //TODO Reregister function

    public abstract  EventHandler<NetworkModule.NewMessageReceived> onNewMessage();

    public abstract EventHandler<CloudletModelMsg> onCloudletModelMsg();

    public abstract  EventHandler<PeriodicTimerModule.PeriodExpired> onPeriodExpired();





    public static final class CloudletModelMsg extends Event {
        private final String cloudlet;
        private final long seq;
        private final Object cloudletModel;
        private final PriorityQueue<CloudletWithPriority> cloudletList;

        public CloudletModelMsg(String cloudlet, long seq, Object cloudletModel, PriorityQueue<CloudletWithPriority> cloudletList) {
            this.cloudlet = cloudlet;
            this.seq = seq;
            this.cloudletModel = cloudletModel;
            this.cloudletList = cloudletList;
        }

        public String getCloudlet() {
            return cloudlet;
        }

        public long getSeq() {
            return seq;
        }

        public PriorityQueue<CloudletWithPriority> getCloudletList() {
            return cloudletList;
        }

        public Object getCloudletModel() {
            return cloudletModel;
        }
    }


    public SSIoTModule(IProcess process, Map<String,Object> props) throws MissingModule {
        super(process,props);
        this.periodicity = (int) props.get("periodicity");
        this.modelUpdatePeriodicity = (int) props.get("modelUpdatePeriodicity");
        this.timeout = (int) props.get("timeout");

        require(NetworkModule.class);
        subscribe(onNewMessage(), NetworkModule.NewMessageReceived.class);

        require(PeriodicTimerModule.class);
        subscribe(onPeriodExpired(), PeriodicTimerModule.PeriodExpired.class);

        subscribe(onCloudletModelMsg(),CloudletModelMsg.class);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\t\tmsgseq:"+msgseq);
        sb.append("\n\t\t\tlastUpdate:"+lastUpdate);
        sb.append("\n\t\t\tmodel:"+model);
        sb.append("\n\t\t\tcloudletModel:"+cloudletModel);
        sb.append("\n\t\t\tcloudletList:"+cloudletList);
        return sb.toString();
    }

    public Object getModel() {
        return model;
    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> state = new HashMap<>();
        state.put("msgseq",msgseq);
        state.put("modelUpdatePeriod",modelUpdatePeriodicity);
        state.put("cloudletModel",cloudletModel);
        state.put("model",model);
        state.put("lastUpdate",lastUpdate);
        state.put("cloudletList",cloudletList);
        state.put("MSG",MSG);
        return state;
    }

    public int getModelUpdatePeriodicity() {
        return modelUpdatePeriodicity;
    }
}
