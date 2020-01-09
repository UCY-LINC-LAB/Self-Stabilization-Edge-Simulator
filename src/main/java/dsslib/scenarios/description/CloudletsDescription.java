package dsslib.scenarios.description;

import java.util.List;

public class CloudletsDescription {
    private float speed = 1.0f;
    private String link_to_other_cloudlets;
    private List<ModulesDescription> modules;
    private List<ZonesDescription> zones;

    public String getLink_to_other_cloudlets() {
        return link_to_other_cloudlets;
    }

    public void setLink_to_other_cloudlets(String link_to_other_cloudlets) {
        this.link_to_other_cloudlets = link_to_other_cloudlets;
    }

    public List<ModulesDescription> getModules() {
        return modules;
    }

    public float getSpeed() {
        return speed;
    }

    public void setModules(List<ModulesDescription> modules) {
        this.modules = modules;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }

    public List<ZonesDescription> getZones() {
        return zones;
    }

    public void setZones(List<ZonesDescription> zones) {
        this.zones = zones;
    }

    @Override
    public String toString() {
        return "CloudletsDescription{" +
                "speed=" + speed +
                ", modules=" + modules +
                '}';
    }
}
