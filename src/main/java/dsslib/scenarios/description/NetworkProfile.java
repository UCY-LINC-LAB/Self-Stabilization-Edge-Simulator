package dsslib.scenarios.description;

public class NetworkProfile {
    private LinkSpeedDescription speed;
    private int downstreamBandwidth;
    private int upstreamBandwidth;

    public LinkSpeedDescription getSpeed() {
        return speed;
    }

    public void setSpeed(LinkSpeedDescription speed) {
        this.speed = speed;
    }

    public int getDownstreamBandwidth() {
        return downstreamBandwidth;
    }

    public void setDownstreamBandwidth(int downstreamBandwidth) {
        this.downstreamBandwidth = downstreamBandwidth;
    }

    public int getUpstreamBandwidth() {
        return upstreamBandwidth;
    }

    public void setUpstreamBandwidth(int upstreamBandwidth) {
        this.upstreamBandwidth = upstreamBandwidth;
    }
}
