package dsslib.selfstabilization;

public class IoTMsgToCloudlet {
    private long msgseq;
    private Object model;

    public IoTMsgToCloudlet(long msgseq, Object model) {
        this.msgseq = msgseq;
        this.model = model;
    }

    public long getMsgseq() {
        return msgseq;
    }

    public Object getModel() {
        return model;
    }

    public void setModel(Object model) {
        this.model = model;
    }

    public void setMsgseq(long msgseq) {
        this.msgseq = msgseq;
    }

    @Override
    public String toString() {
        return "IoTMsgToCloudlet{" +
                "msgseq=" + msgseq +
                ", model=" + model +
                '}';
    }
}
