package dsslib.statistics;

import dsslib.scheduler.Scheduler;

import java.util.Map;
import java.util.TreeMap;

public class SharedRegisterStatistic extends Statistic{

    private Map<Long,Integer> countSharedRegisterMsgs;

    public SharedRegisterStatistic(String id) {
        super(id);
    }

    @Override
    public void apply(Map<String, Object> props) {
        if(countSharedRegisterMsgs==null)
            countSharedRegisterMsgs = new TreeMap<>();

        long globalTime = Scheduler.getInstance().getTime();
        countSharedRegisterMsgs.merge(globalTime,1,Integer::sum);
    }

    public Map<Long, Integer> getCountSharedRegisterMsgs() {
        return countSharedRegisterMsgs;
    }
}
