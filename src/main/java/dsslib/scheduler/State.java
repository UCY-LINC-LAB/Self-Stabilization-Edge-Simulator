package dsslib.lib.core.scheduler;

import java.util.Properties;

public class State {
    Properties properties = new Properties();

    public Object get(String key){
        return properties.get(key);
    }
    public void set(String key,Object value){
        properties.put(key,value);
    }

    @Override
    public String toString() {
        return properties.toString();
    }
}
