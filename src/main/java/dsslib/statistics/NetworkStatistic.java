package dsslib.statistics;

import dsslib.components.networking.OperationPacket;
import dsslib.scheduler.Scheduler;
import dsslib.plots.DataFrame;
import dsslib.utilities.Tuple2;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class NetworkStatistic extends Statistic{

    private Map<FromToEntry,Long> newSendMessages = new HashMap<>();
    private Map<Long,Map<String, Tuple2>> countMsgs = new TreeMap<>();

    public static class FromToEntry{
        private String from;
        private String to;
        private boolean rightDir;

        FromToEntry(String from, String to,boolean rightDir) {
            this.from = from;
            this.to = to;
            this.rightDir = rightDir;
        }
        static class OrderByName implements Comparator<Map.Entry<FromToEntry,Long>> {
            @Override
            public int compare(Map.Entry<FromToEntry, Long> v1, Map.Entry<FromToEntry, Long> v2) {
                String f1 = v1.getKey().from;
                String f2 = v2.getKey().from;
                if(f1.equals(f2))
                    return v1.getKey().to.compareTo(v2.getKey().to);
                return f1.compareTo(f2);
            }
        }

        public String getFrom() {
            return from;
        }

        public String getTo() {
            return to;
        }

        @Override
        public boolean equals(Object o) {
            if(!(o instanceof FromToEntry))
                return false;
            FromToEntry other = (FromToEntry) o;
            return other.from.equals(from) && other.to.equals(to);
        }

        @Override
        public int hashCode() {
            return from.hashCode()+to.hashCode();
        }

        @Override
        public String toString() {
            return '(' + from + ((rightDir)?"->":"<-")+to + ')';
        }
    }
    public NetworkStatistic(String id) {
        super(id);
    }

    @Override
    public void apply(Map<String, Object> props) {
        String from = (String) props.get("from");
        String to = (String) props.get("to");
        OperationPacket packet = (OperationPacket) props.get("packet");

        long globalTime = Scheduler.getInstance().getTime();
        FromToEntry entry = new FromToEntry(from,to,true);
        newSendMessages.merge(entry,1L,Long::sum);

        Map<String, Tuple2> messages = countMsgs.getOrDefault(globalTime, new HashMap<>());

        switch(packet.getPlane()){
            case SHARED_REGISTER_WRITE:
                Tuple2 tp = messages.getOrDefault("SHARED_REGISTER_WRITE",new Tuple2(0,0L));
                messages.putIfAbsent("SHARED_REGISTER_WRITE",tp);
                tp.setR1((int)tp.getR1()+1);
                tp.setR2((long)tp.getR2()+packet.getSize());
                break;
            case SHARED_REGISTER_READ:
                tp = messages.getOrDefault("SHARED_REGISTER_READ",new Tuple2(0,0L));
                messages.putIfAbsent("SHARED_REGISTER_READ",tp);
                tp.setR1((int)tp.getR1()+1);
                tp.setR2((long)tp.getR2()+packet.getSize());
                break;
            case HEALTH_CHECK:
                tp = messages.getOrDefault("HEALTH_CHECK",new Tuple2(0,0L));
                messages.putIfAbsent("HEALTH_CHECK",tp);
                tp.setR1((int)tp.getR1()+1);
                tp.setR2((long)tp.getR2()+packet.getSize());
                break;
            case CONTROL:
                tp = messages.getOrDefault("CONTROL",new Tuple2(0,0L));
                messages.putIfAbsent("CONTROL",tp);
                tp.setR1((int)tp.getR1()+1);
                tp.setR2((long)tp.getR2()+packet.getSize());
                break;
            case DATA:
                tp = messages.getOrDefault("DATA",new Tuple2(0,0L));
                messages.putIfAbsent("DATA",tp);
                tp.setR1((int)tp.getR1()+1);
                tp.setR2((long)tp.getR2()+packet.getSize());
                break;
        }
        countMsgs.putIfAbsent(globalTime,messages);
    }

    public Map<FromToEntry, Long> getNewSendMessages() {
        return newSendMessages;
    }

    public Map<String,? extends Object> toJson(){
        Map<String, Object> result = new HashMap<>();

        //Transmitted
        Map<String, Map<String,Object>> map = new HashMap<>();
        for( Map.Entry<FromToEntry,Long> entry: newSendMessages.entrySet()){
            FromToEntry key = entry.getKey();
            Long value = entry.getValue();
            Map<String, Object> orDefault = map.getOrDefault(key.from, new HashMap<>());
            map.putIfAbsent(key.from,orDefault);
            orDefault.put(key.to,value);
        }
        result.put("transmitted",map);


        //Count
        result.put("over_time",countMsgs);

        return result;
    }

    public DataFrame timeMsgstoDataFrame(){
        DataFrame df = new DataFrame("time",
                "control_msgs","control_msgs_sz",
                "health_checks","health_checks_sz",
                "data_msgs","data_msgs_sz",
                "sr_read","sr_read_sz",
                "sr_write","sr_write_sz"
        );
        Map<Long, Map<String, Tuple2>> control = Statistics.getInstance().getNetworkStatistic().getCountMsgs();
        for(Map.Entry<Long, Map<String, Tuple2>> v : control.entrySet()){
            long time = v.getKey();
            Map<String, Tuple2> value = v.getValue();
            Tuple2 controlV = value.getOrDefault("CONTROL",new Tuple2(0,0L));
            Tuple2 health_check = value.getOrDefault("HEALTH_CHECK",new Tuple2(0,0L));
            Tuple2 sr_read = value.getOrDefault("SHARED_REGISTER_READ",new Tuple2(0,0L));
            Tuple2 sr_write = value.getOrDefault("SHARED_REGISTER_WRITE",new Tuple2(0,0L));
            Tuple2 dataV = value.getOrDefault("DATA",new Tuple2(0,0L));
            df.add(time,
                    controlV.getR1(),controlV.getR2(),
                    health_check.getR1(),health_check.getR2(),
                    dataV.getR1(),dataV.getR2(),
                    sr_read.getR1(),sr_read.getR2(),
                    sr_write.getR1(),sr_write.getR2());
        }

        return df;
    }

    public String exchanges(){
        return newSendMessages.entrySet().stream()
                .filter(e->!e.getKey().getFrom().startsWith("iot_")||!e.getKey().getTo().startsWith("iot_"))
                .map(entry -> {
                    NetworkStatistic.FromToEntry key = entry.getKey();
                    String from = key.getFrom();
                    String to = key.getTo();
                    long value = entry.getValue();
                    return from + "," + to + "," + value;
                }).collect(Collectors.joining("\n"));
    }
    public Map<Long, Map<String, Tuple2>> getCountMsgs() {
        return countMsgs;
    }
}
