package dsslib.components.timers;

import dsslib.process.IProcess;
import dsslib.components.AbstractModule;
import dsslib.events.Event;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.events.Init;
import dsslib.exceptions.MissingModule;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public abstract class SimpleTimerModule extends AbstractModule {

    public abstract EventHandler<Execution> onExecution();
    public abstract EventHandler<Start> onStart();

    @Override
    public EventHandler<Init> onInit() {
        return (e)->{};
    }

    public static final class Start extends Event {
        private final int duration;
        private String uuid;

        public Start(int duration){
            this(duration,UUID.randomUUID().toString());
        }

        public Start(int duration, String uuid){
            this.duration = duration;
            this.uuid = uuid;
        }

        public String getUuid() {
            return uuid;
        }

        public String getUuidShort() {
            if(uuid.length()>15)
                return this.uuid.substring(0,15);
            return uuid;
        }
        public int getDuration() {
            return duration;
        }

    }

    /**
     * Indication Event
     */
    public static final class Expire extends Event {
        private String uuid;
        public Expire(String uuid){this.uuid = uuid;
        }

        public String getUuid() {
            return uuid;
        }
        public String getUuidShort() {
            if(uuid.length()>15)
                return uuid.substring(0,15);
            return uuid;
        }
    }



    public SimpleTimerModule(IProcess process) throws MissingModule {
        super(process,null);
        subscribe(onStart(), Start.class);
    }

    @Override
    public Map<String, Object> getState() {
        Map<String,Object> state = new HashMap<>();
        return state;
    }
}
