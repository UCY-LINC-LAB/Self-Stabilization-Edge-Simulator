package dsslib.components;
import dsslib.process.IProcess;
import dsslib.events.EventHandler;
import dsslib.events.Execution;
import dsslib.events.Init;
import dsslib.exceptions.MissingModule;

import java.util.HashMap;
import java.util.Map;

public class DebugComponent extends DebugModule {
    public DebugComponent(IProcess process, Map<String, Object> props) throws MissingModule {
        super(process, props);
    }

    @Override
    public EventHandler<Init> onInit() {
        return (e)->{
            logToScheduler("Debug Module Initialized");
        };
    }

    @Override
    public EventHandler<Execution> onExecution() {
        return (e)->{

        };
    }

    @Override
    public Map<String, Object> getState() {
        Map <String,Object> res = new HashMap<>();
        return res;
    }
}
