package dsslib.selfstabilization;

public class AggregatedDataModel {

    private String id;
    private Object aggregateInfo;

    public AggregatedDataModel(String id, Object aggregateInfo) {
        this.id = id;
        this.aggregateInfo = aggregateInfo;
    }

    public Object getAggregateInfo() {
        return aggregateInfo;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setAggregateInfo(Object aggregateInfo) {
        this.aggregateInfo = aggregateInfo;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\t\t("+id+","+aggregateInfo+")");
        return sb.toString();
    }
}
