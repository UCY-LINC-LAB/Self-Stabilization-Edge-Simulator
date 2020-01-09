package dsslib.scenarios;

import java.util.Map;
import java.util.Set;

public class SimulationMode {


    public Set<String> getLogs() {
        return logs;
    }

    public void setLogs(Set<String> logs) {
        this.logs = logs;
    }

    private Set<String> statistics;
    private int steps=-1;
    private int progress_every=1000;
    private int statistics_every=1000;
    private int statistics_after=0;
    private int plots_every=1000;
    private Set<String> logs;
    private Set<String> trace_events;
    private Map<String,Object> trace;

    public Set<String> getTrace_events() {
        return trace_events;
    }

    public int getStatistics_after() {
        return statistics_after;
    }

    public void setStatistics_after(int statistics_after) {
        this.statistics_after = statistics_after;
    }

    public void setTrace_events(Set<String> trace_events) {
        this.trace_events = trace_events;
    }

    public int getStatistics_every() {
        return statistics_every;
    }

    public void setStatistics_every(int statistics_every) {
        this.statistics_every = statistics_every;
    }

    private boolean gui;
    private boolean logsEnabled;

    public boolean isGui() {
        return gui;
    }

    public void setGui(boolean gui) {
        this.gui = gui;
    }

    public Map<String, Object> getTrace() {
        return trace;
    }

    public void setTrace(Map<String, Object> trace) {
        this.trace = trace;
    }

    public boolean isLogsEnabled() {
        return logsEnabled;
    }

    public void setLogsEnabled(boolean logsEnabled) {
        this.logsEnabled = logsEnabled;
    }

    public int getProgress_every() {
        return progress_every;
    }

    public void setProgress_every(int progress_every) {
        this.progress_every = progress_every;
    }

    public int getSteps() {
        return steps;
    }

    public void setSteps(int steps) {
        this.steps = steps;
    }


    public Set<String> getStatistics() {
        return statistics;
    }

    public void setStatistics(Set<String> statistics) {
        this.statistics = statistics;
    }


    @Override
    public String toString() {
        return "SimulationMode{" +
                "statistics=" + statistics +
                ", steps=" + steps +
                ", progress_every=" + progress_every +
                ", logs=" + logs +
                ", trace=" + trace +
                ", gui=" + gui +
                ", logsEnabled=" + logsEnabled +
                '}';
    }

    public int getPlots_every() {
        return plots_every;
    }

    public void setPlots_every(int plots_every) {
        this.plots_every = plots_every;
    }
}
