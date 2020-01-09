package dsslib.plots;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class DataFrame {
    private List<Observation> observations;
    private String[] headers;
    public DataFrame(String...headers){
        this.headers = headers;
        this.observations = new LinkedList<>();
    }
    public DataFrame add(Object ...observation){
        this.observations.add(new Observation(headers.length).addFeatures(observation));
        return this;
    }

    @Override
    public String toString() {
        String header = Arrays.asList(headers).stream().collect(Collectors.joining(","));
        String data = observations.stream().map(e->e.toString()).collect(Collectors.joining("\n"));
        return header+"\n"+data;

    }
}
