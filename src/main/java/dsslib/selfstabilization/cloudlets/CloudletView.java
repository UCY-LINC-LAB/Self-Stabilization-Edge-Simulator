package dsslib.selfstabilization.cloudlets;


import dsslib.selfstabilization.SeqIdPair;

import java.util.HashSet;
import java.util.Set;

public class CloudletView {

    /** (lLeader, rnd) **/
    private SeqIdPair ID = new SeqIdPair(-1,"");
    private int  cnt;
    private Set<String> set = new HashSet<>();

    public SeqIdPair getID() {
        return ID;
    }

    public void setID(SeqIdPair ID) {
        this.ID = ID;
    }

    public void setCnt(int cnt) {
        this.cnt = cnt;
    }

    public int getCnt() {
        return cnt;
    }

    public Set<String> getSet() {
        return set;
    }

    public void setSet(Set<String> set) {
        this.set = set;
     }

    @Override
    public boolean equals(Object o) {
        if(! (o instanceof CloudletView))
            return false;
        CloudletView oo = (CloudletView)o;

        if(oo.getSet().containsAll(this.getSet())  && this.getSet().containsAll(oo.getSet()))
            return true;

        if(oo.getID().equals(this.getID()))
            return true;

        return false;
    }

    @Override
    public String toString() {
        return "CloudletView{" +
                "ID=" + ID +
                ", cnt=" + cnt +
                ", set=" + set +
                '}';
    }

    public CloudletView copy() {
        CloudletView view = new CloudletView();
        view.setSet(new HashSet<>(this.set));
        view.setCnt(this.cnt);
        view.setID(new SeqIdPair(this.getID().getSeq(),this.getID().getId()));
        return view;
    }
}
