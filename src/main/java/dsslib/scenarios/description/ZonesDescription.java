package dsslib.scenarios.description;

import java.util.Map;

public class ZonesDescription {
    private String zone;
    private int count;
    private Map<String,String> links;

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public int getCount() {
        return count;
    }

    public String getZone() {
        return zone;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    @Override
    public String toString() {
        return "ZonesDescription{" +
                "zone='" + zone + '\'' +
                ", count=" + count +
                '}';
    }
}
