package dsslib.components.timers;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.exceptions.MissingModule;

import java.util.HashMap;
import java.util.Map;

public abstract class PeriodicTimerModule extends AbstractModule {

    public abstract EventHandler<SimpleTimerModule.Expire> onExpire();
    public abstract EventHandler<PeriodStart> onStart();

    public static final class PeriodExpired extends Event {
        private final String uuid;

        public PeriodExpired(String uuid) {
            this.uuid = uuid;
        }

        public String getUuidShort() {
            if(uuid.length()>15)
                return uuid.substring(0,15);
            return uuid;
        }

        public String getUuid() {
            return uuid;
        }
    }
    public static final class PeriodStart extends Event {
        private final String uuid;
        private final int period;

        public PeriodStart(String uuid, int period) {
            this.uuid = uuid;
            this.period = period;
        }

        public String getUuid() {
            return uuid;
        }
        public String getUuidShort() {
            if(uuid.length()>15)
                return uuid.substring(0,15);
            return uuid;
        }

        public int getPeriod() {
            return period;
        }
    }

    public PeriodicTimerModule(IProcess process) throws MissingModule {
        super(process,null);
        require(SimpleTimerModule.class);
        subscribe(onExpire(), SimpleTimerModule.Expire.class);
        subscribe(onStart(), PeriodStart.class);

    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> state = new HashMap<>();
        return state;
    }
}
