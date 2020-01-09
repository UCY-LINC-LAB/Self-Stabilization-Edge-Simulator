package dsslib.logs;

public class LogEntry {
    private String moduleRealName;
    private long globalTime;
    private long localTime;
    private String process;
    private String module;
    private String log;

    public LogEntry(String moduleName, long globalTime, long localTime, String process, String module, String log) {
        this.moduleRealName = moduleName;
        this.globalTime = globalTime;
        this.localTime = localTime;
        this.process = process;
        this.module = module;
        this.log = log;
    }

    public long getGlobalTime() {
        return globalTime;
    }

    public void setGlobalTime(long globalTime) {
        this.globalTime = globalTime;
    }

    public long getLocalTime() {
        return localTime;
    }

    public void setLocalTime(long localTime) {
        this.localTime = localTime;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getModuleRealName() {
        return moduleRealName;
    }

    public void setModuleRealName(String moduleRealName) {
        this.moduleRealName = moduleRealName;
    }

    public String getLog() {
        return log;
    }

    public void setLog(String log) {
        this.log = log;
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "moduleRealName='" + moduleRealName + '\'' +
                ", log='" + log + '\'' +
                '}';
    }
}
