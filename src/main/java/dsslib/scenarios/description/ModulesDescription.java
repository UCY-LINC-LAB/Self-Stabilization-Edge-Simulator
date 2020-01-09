package dsslib.scenarios.description;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModulesDescription {
    private String module;
    private String implementation;
    private Map<String,Object> params = new LinkedHashMap<>(); //Keeps the order keys have been inserted


    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getImplementation() {
        return implementation;
    }

    public void setImplementation(String implementation) {
        this.implementation = implementation;
    }

    @Override
    public String toString() {
        return "ModulesDescription{" +
                "module='" + module + '\'' +
                ", implementation='" + implementation + '\'' +
                '}';
    }
}
