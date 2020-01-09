package dsslib.scenarios.description;

import java.util.List;

public class IotsDescription {
    private float speed = 1.0f;
    private List<ModulesDescription> modules;
    private List<ZonesDescription> zones;

    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public List<ModulesDescription> getModules() {
        return modules;
    }

    public void setModules(List<ModulesDescription> modules) {
        this.modules = modules;
    }

    public List<ZonesDescription> getZones() {
        return zones;
    }

    public void setZones(List<ZonesDescription> zones) {
        this.zones = zones;
    }
}
