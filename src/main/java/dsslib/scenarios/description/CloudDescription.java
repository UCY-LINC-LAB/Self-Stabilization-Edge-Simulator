package dsslib.scenarios.description;

import java.util.List;

public class CloudDescription {

    private float speed = 1.0f;
    private List<ModulesDescription> modules;

    public List<ModulesDescription> getModules() {
        return modules;
    }

    public void setModules(List<ModulesDescription> modules) {
        this.modules = modules;
    }

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    @Override
    public String toString() {
        return "CloudDescription{" +
                "modules=" + modules +
                '}';
    }
}
