package dsslib.utilities;

public class Tuple2 implements Comparable<Tuple2> {
    private Object r1;
    private Object r2;

    public Tuple2(Object r1, Object r2){
        this.r1 = r1;
        this.r2 = r2;
    }

    public Object getR1() {
        return r1;
    }

    public void setR1(Object r1) {
        this.r1 = r1;
    }

    public Object getR2() {
        return r2;
    }

    public void setR2(Object r2) {
        this.r2 = r2;
    }

    @Override
    public String toString() {
        return "["+r1+','+r2+"]";
    }

    @Override
    public int compareTo(Tuple2 tuple2) {
        Comparable t1 = (Comparable) this.r1;
        Comparable t2 = (Comparable) tuple2.r1;
        return -1*t1.compareTo(t2);
    }
}
