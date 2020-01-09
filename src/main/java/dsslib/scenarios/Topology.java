package dsslib.scenarios;

import dsslib.components.networking.ChannelModule;
import dsslib.exceptions.NoRequiredModuleFound;
import dsslib.process.IProcess;
import dsslib.scheduler.Scheduler;

import java.util.*;

/**
 * A topology should know the processes and how they are linked
 *
 */
public abstract class Topology {

    /** All Processes **/
    private final Map<String, IProcess> processes = new HashMap<>();

    private final Set<String> allavailableModules = new LinkedHashSet<>();

    /** The set of links **/
    private final Map<String,IProcess> links = new HashMap<>();

    public Map<String, IProcess> getProcesses() {
        return processes;
    }

    public Map<String, IProcess> getLinks() {
        return links;
    }

    public Optional<IProcess> findProcess(String id){
        IProcess process = processes.get(id);
        if(process==null)
            return Optional.empty();
        return Optional.of(process);

    };

    /**
     * Registers the process
     * @param process
     */
    public void registerProcess(IProcess process) throws NoRequiredModuleFound {
        this.processes.put(process.getUuid(),process);

        if(process.containsModule(ChannelModule.class)) {
            Scheduler.getInstance().register(process, false);
            links.put(process.getUuid(), process);

        }else{
            Scheduler.getInstance().register(process,true);

        }

        process.getModules().values().stream().map(c->c.getClass().getSuperclass().getName()).forEach(allavailableModules::add);


    }

    public abstract Map<String,Object> getState();

    /**
     * Return all the available modules
     * @return
     */
    public Set<String> getAvailableModules(){
       return allavailableModules;
    }
}
