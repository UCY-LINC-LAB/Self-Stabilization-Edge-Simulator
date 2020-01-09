package dsslib.logs;

import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;

public class EventTracer implements Closeable {
    private final String fileName;
    private final boolean clearAll;

    private final PrintWriter pw;

    public EventTracer(String dir, String fileName, boolean clearAll) throws IOException {
        this.fileName = fileName;
        this.clearAll = clearAll;
        Paths.get(dir).toFile().mkdirs();
        pw = new PrintWriter(new FileWriter(Paths.get(dir,fileName+".trace").toFile(),!clearAll));

    }

    public void flush(String text){
        pw.write(text);
        pw.flush();
    }
    boolean state = false;

    public void flushOnlyIf(String text){
        if(state) {
            pw.write(text);
            pw.flush();
        }
    }
    public void setState(boolean state){
        this.state = state;
    }

    @Override
    public void close() throws IOException {
        pw.close();
    }
}
