package dsslib.scenarios.description;

public class ProcessesDescription {
    private CloudDescription cloud;
    private CloudletsDescription cloudlets;
    private IotsDescription iots;


    public CloudDescription getCloud() {
        return cloud;
    }

    public void setCloud(CloudDescription cloud) {
        this.cloud = cloud;
    }

    public CloudletsDescription getCloudlets() {
        return cloudlets;
    }

    public IotsDescription getIots() {
        return iots;
    }

    public void setCloudlets(CloudletsDescription cloudlets) {
        this.cloudlets = cloudlets;
    }

    public void setIots(IotsDescription iots) {
        this.iots = iots;
    }

    @Override
    public String toString() {
        return "ProcessesDescription{" +
                "cloud=" + cloud +
                ", cloudlets=" + cloudlets +
                ", iots=" + iots +
                '}';
    }
}
