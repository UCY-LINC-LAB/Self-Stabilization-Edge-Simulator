package dsslib.selfstabilization;

import java.util.*;
import java.util.stream.Collectors;

public class RegisterInfoModel {

    /**
     * A set (bounded by deviceSetSize) of IoT devices, their models and the information needed for failure
     * detection;
     */
    private Map<String,IoTModelTime> devices = new HashMap<>(); // IoT Devices and their models

    /**
     * cloudlets is a set (bounded by cloudletSetSize) of cloudlets and the information needed for
     * failure detection
     */
    private Map<String, Integer> cloudlets = new HashMap<>(); // A set of cloudlets

    /**
     * Has the Form (seq,id). Is the cloudlets' current leader
     */
    private SeqIdPair leader = new SeqIdPair(-1,""); //

    /**
     * A set of cloudlet IDs that have been selected as guards
     */
    private Set<String> guards = new LinkedHashSet<>();


    /**
     * Retrieve a new cloudlets list
     * @return
     */
    public Map<String,Integer> getCloudletsCopy() {
        if(cloudlets.isEmpty())
            return new HashMap<>();
        return cloudlets.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    public Map<String,Integer> getCloudlets() {
        return cloudlets;
    }

    /**
     * Retrieve a new copy
     * @return
     */
    public Map<String,IoTModelTime> getDevices() {
        return devices;
    }
    /**
     * Retrieve a new copy
     * @return
     */
    public Map<String,IoTModelTime> getDevicesCopy() {
        if(devices.isEmpty())
            return new HashMap<>();
        Map<String,IoTModelTime> devices = new HashMap<>();
        devices.entrySet().forEach(e->{
            devices.put(e.getKey(),e.getValue().copy());
        });
        return devices;
    }

    public Set<String> getGuards() {
        return guards;
    }
    public Set<String> getGuardsCopy() {
        return new HashSet<>(guards);
    }

    public SeqIdPair getLeader() {
        return leader;
    }

    public void setCloudlets(Map<String, Integer> cloudlets) {
        this.cloudlets = cloudlets;
    }

    public void setDevices(Map<String,IoTModelTime> devices) {
        this.devices = devices;
    }

    public void setGuards(Set<String> guards) {
        this.guards = guards;
    }

    public void setLeader(SeqIdPair leader) {
        this.leader = leader;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\t\t\tDevices:\n\t\t\t\t"+devices.entrySet().stream().map(e->e.getKey()+":"+e.getValue())
                .collect(Collectors.joining("\n\t\t\t\t")));
        sb.append("\n\t\t\tCloudlets:\n\t\t\t\t"+cloudlets.entrySet().stream().map(e->e.getKey()+":"+e.getValue())
                .collect(Collectors.joining("\n\t\t\t\t")));
        sb.append("\n\t\t\tGuards:\n\t\t\t\t"+ guards.stream()
                .collect(Collectors.joining("\n\t\t\t\t")));
        sb.append("\n\t\t\tLeader:\n\t\t\t\t"+leader);
        return sb.toString();
    }

    public boolean isNull(){
        if(!devices.isEmpty())
            return false;
        if(!cloudlets.isEmpty())
            return false;
        if(!guards.isEmpty())
            return false;
        if(!leader.getId().isEmpty())
            return false;
        return true;
    }
    @Override
    public boolean equals(Object o) {
        if(!( o instanceof RegisterInfoModel ))
            return false;

        RegisterInfoModel rr = (RegisterInfoModel) o;

        if(!this.getLeader().equals(rr.getLeader()))
            return false;

        if(!this.guards.containsAll(rr.guards) || !rr.guards.containsAll(guards))
            return false;

        if(!this.cloudlets.keySet().containsAll(rr.cloudlets.keySet()) || !rr.cloudlets.keySet().containsAll(cloudlets.keySet()))
            return false;

        if(!this.devices.keySet().containsAll(rr.devices.keySet()) || !rr.devices.keySet().containsAll(devices.keySet()))
            return false;

        return true;

    }

}
