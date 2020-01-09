package dsslib.plots;

import dsslib.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

public class ProcessExecutor implements Callable<String> {

    private static final Logger logger = LoggerFactory.getLogger(ProcessExecutor.class);

    private final ProcessBuilder processBuilder;
    private String cmd;
    private byte[] input;
    private long time;

    public ProcessExecutor(String...cmd){
        processBuilder = new ProcessBuilder();
        if(cmd.length!=0)
            processBuilder.command(cmd);
        this.cmd = Arrays.asList(cmd).stream().limit(3).collect(Collectors.joining(" "))+" ...";
        time = Scheduler.getInstance().getTime();
    }
    public ProcessExecutor command(String ...cmd){
        processBuilder.command(cmd);
        this.cmd = Arrays.asList(cmd).stream().limit(3).collect(Collectors.joining(" "))+" ...";
        return this;
    }
    public ProcessExecutor setInputData(byte[] data){
        this.input = data;
        return this;
    }

    @Override
    public String call() throws Exception {

        Process process = processBuilder.start();
        if(input!=null) {
            OutputStream outputStream = process.getOutputStream();
            outputStream.write(this.input);
            outputStream.close();
        }


        String output = read(process.getInputStream());
        int exitVal = process.waitFor();
        if (exitVal == 0) {
            logger.info("["+time +"] Process exited normally: "+cmd);
        } else {
            logger.info("["+time+"] Process exited with code:"+exitVal+": "+cmd);
            String err = read(process.getErrorStream());
            logger.info(err);
            return err;
        }
        return output;
    }

    public String read(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        BufferedReader reader = new BufferedReader( new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line + "\n");
        }
        return output.toString();

    }
}
