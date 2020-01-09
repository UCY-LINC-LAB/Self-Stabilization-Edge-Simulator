package dsslib.plots;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Observation {
    private Object columns[];

    public Observation(int features) {
        this.columns = new Object[features];
    }

    public Observation addFeatures(Object...features) {
        int size = Math.min(this.columns.length, features.length);
        for (int i = 0; i < size; i++) {
            this.columns[i] = features[i];
        }
        return this;
    }

    @Override
    public String toString() {
        return Arrays.asList(columns).stream().map(e->e.toString()).collect(Collectors.joining(","));
    }
}
