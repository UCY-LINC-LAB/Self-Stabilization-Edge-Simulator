package dsslib.scenarios.description;

import java.util.Map;

public class LinkSpeedDescription {
    private String type;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setProps(Map<String, Object> props) {
        this.props = props;
    }

    private Map<String,Object> props;
}
