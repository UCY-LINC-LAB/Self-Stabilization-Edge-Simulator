package dsslib.scenarios;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;

public class ScenarioDescriptionLoader {
    private static final Logger logger = LoggerFactory.getLogger(ScenarioDescriptionLoader.class);
    private static ScenarioDescription scenarioDescription;

    public static ScenarioDescription getScenarioDescription() {
        return scenarioDescription;
    }

    public static ScenarioDescription load(String path){

        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        logger.info("Loading from:"+Paths.get(path).toAbsolutePath());

        try {
            scenarioDescription = mapper.readValue(new File(path), ScenarioDescription.class);
            return scenarioDescription;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
