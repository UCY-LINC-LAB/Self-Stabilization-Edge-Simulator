package dsslib.selfstabilization;

import java.util.PriorityQueue;

/**
 * This should be sent to an iot device from a cloudlet
 */
public class CloudletList {

    private final long seq;
    private final Object model;
    private final PriorityQueue<CloudletWithPriority> cloudlets;

    public CloudletList(long seq, Object model, PriorityQueue<CloudletWithPriority> cloudlets) {
        this.seq = seq;
        this.model = model;
        this.cloudlets = cloudlets;
    }

    @Override
    public String toString() {
        return "CloudletList{" +
                "seq=" + seq +
                ", model=" + model +
                ", cloudlets=" + cloudlets +
                '}';
    }

    public long getSeq() {
        return seq;
    }

    public Object getModel() {
        return model;
    }

    public PriorityQueue<CloudletWithPriority> getCloudlets() {
        return cloudlets;
    }

}

