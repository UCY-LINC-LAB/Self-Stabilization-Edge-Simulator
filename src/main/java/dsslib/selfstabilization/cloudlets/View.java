package dsslib.selfstabilization.cloudlets;

import java.util.HashSet;
import java.util.Set;

public class View {
    private long ID;
    private Set<String> set = new HashSet<>();

    public long getID() {
        return ID;
    }

    public Set<String> getSet() {
        return set;
    }

    public void setID(long ID) {
        this.ID = ID;
    }

    public void setSet(Set<String> set) {
        this.set = set;
    }

    @Override
    public String toString() {
        return "View{" +
                "ID=" + ID +
                ", set=" + set +
                '}';
    }
}
