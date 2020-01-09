package dsslib.components;

import dsslib.process.IProcess;
import dsslib.exceptions.MissingModule;

import java.util.Map;

public abstract class DebugModule extends AbstractModule{

    public DebugModule(IProcess process, Map<String, Object> props) throws MissingModule {
        super(process, props);
    }
}
