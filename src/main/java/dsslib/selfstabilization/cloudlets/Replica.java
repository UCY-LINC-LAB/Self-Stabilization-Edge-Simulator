package dsslib.selfstabilization.cloudlets;

import dsslib.utilities.Tuple2;
import dsslib.scenarios.ScenarioDescriptionLoader;

import java.util.HashMap;
import java.util.Map;

public class Replica {


    //It doesn't matter if we start with null
    private CloudletStatus status = CloudletStatus.PROPOSE;

    /** Multicast round number **/
    private int rnd = -1;


    /** Replica State **/
    // (  (lLeader,rnd)  , Set<String> )
    private CloudletView view = new CloudletView();

    /**Proposed View **/
    private CloudletView propV = new CloudletView();

    /** Replica state, basically is the aggregated sensory info**/
    private Object state;

    /** Last delivered messages to the sate machine **/
    /** TODO Make sure that I understood correctly what this is **/
    private Map<String, Map<String, Tuple2>> msg = new HashMap<>();


    /** Last fetched input to the state machine **/
    private Map<String, Tuple2> input = new HashMap<>();


    /** Recently live and connected component **/
    private Object FD;


    public void setStatus(CloudletStatus status) {
        this.status = status;
    }

    public CloudletStatus getStatus() {
        return status;
    }

    public void setView(CloudletView view) {
        this.view = view;
    }

    public CloudletView getView() {
        return view;
    }

    public Map<String,Tuple2> getInput() {
        return input;
    }

    public CloudletView getPropV() {
        return propV;
    }

    public void setPropV(CloudletView propV) {
        this.propV = propV;
    }

    public void setInput(Map<String,Tuple2> input) {
        this.input = input;
    }

    public int getRnd() {
        return rnd;
    }

    public Object getState() {
        return state;
    }

    public void setState(Object state) {
        this.state = state;
    }

    public Map<String, Map<String,Tuple2>> getMsg() {
        return msg;
    }

    public void setMsg(Map<String, Map<String,Tuple2>> msg) {
        this.msg = msg;
    }

    public void setRnd(int rnd) {
        this.rnd = rnd;
    }

    @Override
    public String toString() {
        return "Replica{" +
                "status=" + status +
                ", rnd=" + rnd +
                ", view=" + view +
                ", propV=" + propV +
                ", state=" + state +
                ", msg=" + msg +
                ", input=" + input +
                ", FD=" + FD +
                '}';
    }

    public Replica copy() {
        Replica n  = new Replica();
        n.setView(this.getView().copy());
        n.setStatus(this.getStatus());
        n.setState(this.state);
        n.setPropV(this.getPropV().copy());
        n.FD = FD;
        //TODO test input copy replica
        n.input = new HashMap<>(this.input);
        n.rnd = this.rnd;
        n.msg = new HashMap<>(this.msg);
        return n;
    }

    public int calculateSize() {
        int aggregateSize = ScenarioDescriptionLoader.getScenarioDescription().getAggregateSize();
        int recordSize = (int)ScenarioDescriptionLoader.getScenarioDescription().getProperties().getOrDefault("recordSize",32);
        int size = 0;
        size += view.getSet().stream().mapToInt(String::length).sum();
        size += status.toString().length();
        if(this.state!=null)
            size += aggregateSize; //For the state
        size += propV.getSet().stream().mapToInt(String::length).sum();
        //FD should not be used...
        size += aggregateSize; //This was the input
        size +=  4; //for rnd;
        int s = 0;
        for(Map<String,Tuple2> v : msg.values()) {
            if (v != null) {
                s += recordSize * v.size();
            }
        }
        size+=s;
        return size;
    }
}
