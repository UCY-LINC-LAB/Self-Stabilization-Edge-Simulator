package dsslib.components.timers;


import dsslib.process.IProcess;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.events.Init;
import dsslib.exceptions.MissingModule;

import java.util.HashMap;
import java.util.Map;

public class PeriodicTimerComponent extends PeriodicTimerModule {

    Map<String, SimpleTimer.State> states = new HashMap<>();

    @Override
    public EventHandler<SimpleTimerModule.Expire> onExpire() {
        return (e)->{
            String uuid = e.getUuid();
            SimpleTimer.State state = states.get(uuid);
            //Not destined for me
            if(state == null){ return; }

            logToScheduler("Expired for "+e.getUuidShort());

            trigger(new SimpleTimerModule.Start(state.getDuration(),uuid));
            trigger(new PeriodExpired(uuid));
        };
    }

    @Override
    public EventHandler<PeriodStart> onStart() {
        return (e)->{ trigger(new SimpleTimerModule.Start(e.getPeriod(),e.getUuid()));
        SimpleTimer.State state = new SimpleTimer.State();
        state.setDuration(e.getPeriod());
        states.put(e.getUuid(), state);
        logToScheduler("Starting a "+e.getPeriod()+" period for "+e.getUuidShort());
        };
    }

    @Override
    public EventHandler<Init> onInit() {
        return (e)->{ };
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{};
    }

    public PeriodicTimerComponent(IProcess process) throws MissingModule {
        super(process);
    }
}
