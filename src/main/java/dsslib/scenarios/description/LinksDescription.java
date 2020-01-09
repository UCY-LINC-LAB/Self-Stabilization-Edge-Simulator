package dsslib.scenarios.description;

public class LinksDescription {
    private float speed = 1.0f;
    private int downstreamBandwidth;
    private int upstreamBandwidth;


    public float getSpeed() {
        return speed;
    }

    public void setSpeed(float speed) {
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
