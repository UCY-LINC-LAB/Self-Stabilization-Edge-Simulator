package dsslib.selfstabilization;

public class CloudletModel {
    private final String ip;
    private final Object value;

    public CloudletModel(String ip, Object value) {
        this.ip = ip;
        this.value = value;
    }

    public Object getValue() {
        return value;
    }

    public String getIp() {
        return ip;
    }

    @Override
    public String toString() {
        return ip;
    }
}
