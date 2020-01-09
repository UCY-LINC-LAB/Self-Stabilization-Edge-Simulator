package dsslib.selfstabilization;

public class SeqIdPair {

    private int seq;
    private String id;

    public SeqIdPair(int sequence, String id) {
        this.seq = sequence;
        this.id = id;
    }

    public int getSeq() {
        return seq;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setSeq(int seq) {
        this.seq = seq;
    }

    @Override
    public String toString() {
        return "("+seq +","+id+ ')';
    }
}
