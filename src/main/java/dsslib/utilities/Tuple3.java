package dsslib.utilities;

public class Tuple3 {
    private Object r1;
    private Object r2;
    private Object r3;

    public Tuple3(Object r1, Object r2, Object r3) {
        this.r1 = r1;
        this.r2 = r2;
        this.r3 = r3;
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

    public Object getR3() {
        return r3;
    }

    public void setR3(Object r3) {
        this.r3 = r3;
    }

    @Override
    public String toString() {
        return "["+r1+',' +r2+','+r3+']';
    }
}
