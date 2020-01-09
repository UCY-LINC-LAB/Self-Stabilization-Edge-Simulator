package dsslib.components.healthcheck;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.components.networking.NetworkModule;
import dsslib.components.timers.SimpleTimer;
import dsslib.components.timers.SimpleTimerModule;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.exceptions.MissingModule;

import java.util.HashMap;
import java.util.Map;

public abstract class HealthCheckModule extends AbstractModule {

    public abstract EventHandler<SimpleTimer.Expire> onExpire();

    public abstract EventHandler<Start> onStart();
    public abstract EventHandler<Stop> onStop();

    public  abstract EventHandler<NetworkModule.NewMessageReceived> onNewMessage();

    public static class Stop extends Event {
        private final String host;

        public Stop(String host){
            this.host = host;
        }

        public String getHost() {
            return host;
        }
    }

    public static class Start extends Event{
        private final String host;
        private final int period;

        public Start(String host, int period) {
            this.host = host;
            this.period = period;
        }

        public String getHost() {
            return host;
        }

        public int getPeriod() {
            return period;
        }
    }

    public static final class HostIsUp extends Event {
        private String host;

        public HostIsUp(String host) {
            this.host = host;
        }

        public String getHost() {
            return host;
        }

        @Override
        public Event clone() {
            return new HostIsUp(host);
        }
    }
    public static final class HostIsUpButLate extends Event {
        private String host;

        public HostIsUpButLate(String host) {
            this.host = host;
        }

        public String getHost() {
            return host;
        }

        @Override
        public Event clone() {
            return new HostIsUp(host);
        }
    }
    public static final class HostIsDown extends Event {
        private String host;

        public HostIsDown(String host) {
            this.host = host;
        }

        public String getHost() {
            return host;
        }

        @Override
        public Event clone() {
            return new HostIsDown(host);
        }
    }

    public HealthCheckModule(IProcess process) throws MissingModule {
        super(process,null);
        require(NetworkModule.class);
        require(SimpleTimerModule.class);
        subscribe(onStart(),Start.class);
        subscribe(onStop(),Stop.class);
        subscribe(onExpire(),SimpleTimer.Expire.class);
        subscribe(onNewMessage(), NetworkModule.NewMessageReceived.class);
    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> state = new HashMap<>();
        return state;
    }
}
