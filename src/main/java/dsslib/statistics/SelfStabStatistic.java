package dsslib.statistics;

import dsslib.scheduler.Scheduler;

import java.util.*;
import java.util.stream.Collectors;

public class SelfStabStatistic extends Statistic{

    private Map<Long, Map<Statistics.Event, Set<Map<String,Object>>>> eventsStats = new HashMap<>();
    private Queue<String> pending = new LinkedList<>();

    public SelfStabStatistic(String id) {
        super(id);
    }

    public String fetchPending(){
        StringBuilder sb = new StringBuilder();
        while(!pending.isEmpty()){
            String s = pending.remove();
            sb.append(s+"\n");
        }

        return sb.toString();
    }

    @Override
    public synchronized void apply(Map<String, Object> props) {
        Statistics.Event type = (Statistics.Event) props.get("event");
        long time = Scheduler.getInstance().getTime();
        Map<Statistics.Event, Set<Map<String, Object>>> map = eventsStats.getOrDefault(time, new HashMap<>());
        Set<Map<String,Object>>records= map.getOrDefault(type,new HashSet<>());
        props.remove("event");
        records.add(props);
        map.putIfAbsent(type,records);
        eventsStats.putIfAbsent(time,map) ;

        String options = props.entrySet().stream().map(e->e.getKey()+":"+e.getValue()).collect(Collectors.joining(","));
        String fetch = time + "\t"+ type+"\t"+"{"+options+"}";
        pending.add(fetch);

    }

    public Map<Long, Map<Statistics.Event, Set<Map<String, Object>>>> getEventsStats() {
        return eventsStats;
    }
}
