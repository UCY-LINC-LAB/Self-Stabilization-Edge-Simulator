package dsslib.logs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import dsslib.scenarios.ScenarioDescriptionLoader;
import dsslib.scenarios.ScenarioDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Paths;


public class DirectoryService {
    private static final Logger logger = LoggerFactory.getLogger(DirectoryService.class);

    private final File root;

    private static DirectoryService directoryService;

    public static DirectoryService getInstance() {
        return directoryService;
    }

    private DirectoryService() throws Exception {
        ScenarioDescription scenarioDescription = ScenarioDescriptionLoader.getScenarioDescription();
        String root = scenarioDescription.getRoot();
        String name = scenarioDescription.getName();

        long seed = ScenarioDescriptionLoader.getScenarioDescription().getRandom_seed();
        if(seed == 0){
            seed = (long) (Math.random()*10000000);
            ScenarioDescriptionLoader.getScenarioDescription().setRandom_seed(seed);
        }
        String st = String.format("%07d",seed);
        this.root = Paths.get(root,name,st).toFile();
        if(this.root.exists()) {
            logger.info("Directory: " + this.root.getAbsolutePath() + " already exists");
            delete(this.root);
            logger.info("Directory: " + this.root.getAbsolutePath() + " deleted");
        }
        if(this.root.mkdirs()){
            logger.info("Directory: "+this.root.getAbsolutePath()+" created");
        }else{
            throw new Exception("Failed to create directory: "+this.root.getAbsolutePath());

        }
    }


    public static boolean delete(File file) {

        File[] flist = null;

        if(file == null){
            return false;
        }

        if (file.isFile()) {
            return file.delete();
        }

        if (!file.isDirectory()) {
            return false;
        }

        flist = file.listFiles();
        if (flist != null && flist.length > 0) {
            for (File f : flist) {
                if (!delete(f)) {
                    return false;
                }
            }
        }

        return file.delete();
    }

    public static void init() throws Exception {
        directoryService = new DirectoryService();
    }

    public String getSchedulerPath(){
        return this.root.getAbsolutePath();
    }

    public String getTracesPath() throws Exception {
        File dir = Paths.get(this.root.getAbsolutePath(), "traces").toFile();
        if(!dir.exists()){
            if(dir.mkdirs()){
                logger.info("Directory: "+dir.getAbsolutePath()+" created");
            }else{
                throw new Exception("Failed to create directory: "+dir.getAbsolutePath());
            }
        }
        return dir.getAbsolutePath();
    }
    public String getStatisticsPath() throws Exception {
        File dir = Paths.get(this.root.getAbsolutePath(), "stats").toFile();
        if(!dir.exists()){
            if(dir.mkdirs()){
                logger.info("Directory: "+dir.getAbsolutePath()+" created");
            }else{
                throw new Exception("Failed to create directory: "+dir.getAbsolutePath());
            }
        }
        return dir.getAbsolutePath();
    }
    public String getPlotsPath() throws Exception {
        File dir = Paths.get(this.root.getAbsolutePath(), "plots").toFile();
        if(!dir.exists()){
            if(dir.mkdirs()){
                logger.info("Directory: "+dir.getAbsolutePath()+" created");
            }else{
                throw new Exception("Failed to create directory: "+dir.getAbsolutePath());
            }
        }
        return dir.getAbsolutePath();
    }

    public void write( File file, byte[] bytes) throws IOException {
        OutputStream out = new FileOutputStream(file);
        out.write(bytes);
        out.close();
    }

    public void append(File file, String data) throws IOException {
        FileWriter fw = new FileWriter(file,true);
        PrintWriter pw = new PrintWriter(fw);
        pw.print(data);
        pw.close();
    }

    public void writeConfiguration() throws IOException {
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ScenarioDescription desc = ScenarioDescriptionLoader.getScenarioDescription();
        mapper.writeValue(Paths.get(this.root.getAbsolutePath(),"configuration.yml").toFile(),desc);
    }
}
