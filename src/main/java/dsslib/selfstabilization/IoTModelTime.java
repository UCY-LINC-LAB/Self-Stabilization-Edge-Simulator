package dsslib.selfstabilization;

public class IoTModelTime {
    private final String iot;
    private final long time;
    private final Object model;

    public String getIot() {
        return iot;
    }

    public long getTime() {
        return time;
    }

    public Object getModel() {
        return model;
    }

    public IoTModelTime(String iot, long time, Object model) {
        this.iot = iot;
        this.time = time;
        this.model = model;
    }

    @Override
    public String toString() {
        return "("+iot+","+model+","+time+")";
    }

    public IoTModelTime copy() {
        return new IoTModelTime(iot,time,model);
    };
}
