package dsslib;

import dsslib.components.AbstractModule;
import dsslib.scheduler.Scheduler;
import dsslib.logs.DirectoryService;
import dsslib.scenarios.Scenario;
import dsslib.scenarios.ScenarioDescription;
import dsslib.scenarios.ScenarioDescriptionLoader;
import dsslib.statistics.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class App {
    private static final Logger logger = LoggerFactory.getLogger(App.class);
    public static Scenario SCENARIO;
    public static ExecutorService EXECUTORS = Executors.newFixedThreadPool(8);
    public static ExecutorService SINGLE_EXECUTOR = Executors.newSingleThreadExecutor();
    public static void main(String args[]) throws Exception {
        String base = "./scenarios/";
        logger.info("Using scenarios from  directory: "+base);

        String scenarioFile = "default.yml";
        if(args.length==1){
            scenarioFile = args[0];
            if(!scenarioFile.endsWith(".yml"))
                scenarioFile+=".yml";
        }
        String path = Paths.get(base,scenarioFile).toFile().getAbsolutePath();
        logger.info("Using scenario: "+path);

        final ScenarioDescription config = ScenarioDescriptionLoader.load(path);
        SCENARIO = config.build();
        Scheduler.getInstance().setScenario(SCENARIO);

        for(String module : Scheduler.getInstance().getScenario().getMode().getLogs()){
            Class<? extends AbstractModule> clazzImpl = (Class<? extends AbstractModule>) App.class.getClassLoader().loadClass(module);
            Scheduler.getInstance().registerModuleForLog(clazzImpl);
        }

        boolean gui = config.getMode().isGui();

        if (gui) {
            logger.info("Not implemented yet");
        } else{
            int steps = config.getMode().getSteps();
            if (steps <= 0)
                Scheduler.getInstance().startConsole();
            else {
                Scheduler.getInstance().setStarted(true);
                Scheduler.getInstance().multipleSteps(steps);
            }
        }
        Statistics.getInstance().writeSelfStabStatistics();
        DirectoryService.getInstance().writeConfiguration();

        EXECUTORS.shutdown();
        SINGLE_EXECUTOR.shutdown();

    }
}
