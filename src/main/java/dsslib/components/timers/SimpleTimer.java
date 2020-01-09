package dsslib.components.timers;

import dsslib.process.IProcess;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.exceptions.MissingModule;

import java.util.HashMap;
import java.util.Map;

public class SimpleTimer extends SimpleTimerModule {

    Map<String,State> states = new HashMap<>();

    public static class State{
        private int duration;
        private int count;



        public int getDuration() {
            return duration;
        }

        public void inc() {
            this.count++;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public int getCount() {
            return count;
        }
    }
    @Override
    public EventHandler<Start> onStart() {
        return (e)->{
            String uuid = e.getUuid();
            State state = states.getOrDefault(uuid,new State());
            state.setDuration(e.getDuration());
            logToScheduler("Starting for "+e.getUuidShort()+" with local period: " + e.getDuration());
            states.putIfAbsent(uuid,state);
        };
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{
            states.entrySet().forEach(entry ->{
                String uuid = entry.getKey();
                State state = entry.getValue();
                state.inc();

                if(state.getCount()>=state.getDuration()){
                    logToScheduler("Expire is triggered for "+uuid);
                    trigger(new Expire(uuid));
                }

            });
            states.entrySet().removeIf(next -> next.getValue().getCount() >= next.getValue().getDuration());


        };
    }

    public SimpleTimer(IProcess process) throws MissingModule {
        super(process);
    }
}
