package dsslib.statistics;

import dsslib.scheduler.Scheduler;
import dsslib.plots.DataFrame;
import dsslib.utilities.Tuple3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class AggregateState extends Statistic{

    private Map<Long, Set<Tuple3>> states = new TreeMap<>();

    public AggregateState(String id) {
        super(id);
    }

    @Override
    public void apply(Map<String, Object> props) {
        Object state =  props.get("state");
        Object realState =  props.get("realState");
        String uuid = (String) props.get("process");
        long time = Scheduler.getInstance().getTime();
        Set<Tuple3> orDefault = states.getOrDefault(time, new HashSet<>());
        states.putIfAbsent(time,orDefault);

        // Only when it is sum
        if(state==null)
            state = 0;

        orDefault.add(new Tuple3(uuid,state,realState));


    }

    public DataFrame getDataFrame(){
       DataFrame df = new DataFrame("time","process","value","real");
        for(Map.Entry<Long, Set<Tuple3>> v: states.entrySet()) {
            long time = v.getKey();
            Set<Tuple3> value = v.getValue();
            for (Tuple3 c : value) {
                df.add(time, c.getR1(), c.getR2(), c.getR3());
            }
        }
        return df;
    }

    public Map<Long, Set<Tuple3>> getStates() {
        return states;
    }
}
