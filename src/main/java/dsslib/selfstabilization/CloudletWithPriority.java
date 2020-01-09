package dsslib.selfstabilization;

public class CloudletWithPriority implements  Comparable<CloudletWithPriority> {
    private String id;
    private Double value;

    public CloudletWithPriority(String id, Double value) {
        this.id = id;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    @Override
    public int compareTo(CloudletWithPriority cloudletWithOrder) {
        if(value<cloudletWithOrder.value)
            return -1;
        if(value > cloudletWithOrder.value)
            return 1;
        return 0;
    }

    @Override
    public String toString() {
        return "{" +
                "id='" + id + '\'' +
                ", value=" + value +
                '}';
    }
}
