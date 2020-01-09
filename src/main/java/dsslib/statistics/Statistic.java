package dsslib.statistics;

import dsslib.exceptions.MissingScenario;
import dsslib.scenarios.ScenarioDescriptionLoader;

import java.util.Map;

public abstract class Statistic{
    protected final String id;

    protected Statistic(String id) {
        this.id = id;
    }


    protected abstract void apply(Map<String,Object> props);

    public void record(Map<String,Object> props) throws MissingScenario {

        if(ScenarioDescriptionLoader.getScenarioDescription().getMode().getStatistics().contains(this.id))
            apply(props);
    }
    public void record() throws MissingScenario {
        if(ScenarioDescriptionLoader.getScenarioDescription().getMode().getStatistics().contains(this.id))
            apply(null);
    }
}
