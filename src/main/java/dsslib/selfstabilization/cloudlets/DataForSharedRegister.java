package dsslib.selfstabilization.cloudlets;


import dsslib.selfstabilization.SeqIdPair;

public class DataForSharedRegister {
    private final String process;
    private final SeqIdPair leader;
    private final int rnd;
    private final Object state;

    public DataForSharedRegister(String process, SeqIdPair leader, int rnd, Object state) {
        this.process = process;
        this.leader = leader;
        this.rnd = rnd;
        this.state = state;
    }

    public String getProcess() {
        return process;
    }

    public SeqIdPair getLeader() {
        return leader;
    }

    public int getRnd() {
        return rnd;
    }

    public Object getState() {
        return state;
    }

    @Override
    public String toString() {
        return "DataForSharedRegister{" +
                "process='" + process + '\'' +
                ", leader=" + leader +
                ", rnd=" + rnd +
                ", state=" + state +
                '}';
    }
}
