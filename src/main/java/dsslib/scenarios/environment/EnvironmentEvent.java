package dsslib.scenarios.environment;

import java.util.HashMap;
import java.util.Map;

public class EnvironmentEvent implements Comparable<EnvironmentEvent> {
    private long globalTime;
    private EventType eventType;
    private String processId;
    private Map<String,Object> properties = new HashMap<>();

    public EnvironmentEvent(long globalTime, EventType eventType, String processId) {
        this.globalTime = globalTime;
        this.eventType = eventType;
        this.processId = processId;
    }
    public void addProperty(String key,Object value){
        this.properties.put(key,value);
    }
    public Object getProperty(String key){
        return this.properties.get(key);
    }

    public long getGlobalTime() {
        return globalTime;
    }

    public void setGlobalTime(long globalTime) {
        this.globalTime = globalTime;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

    @Override
    public int compareTo(EnvironmentEvent event) {
        if(globalTime < event.globalTime)
            return -1;
        if(globalTime > event.globalTime)
            return 1;
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public String toString() {
        return "Event{" +
                "globalTime=" + globalTime +
                ", eventType=" + eventType +
                ", processId='" + processId + '\'' +
                '}';
    }
}
