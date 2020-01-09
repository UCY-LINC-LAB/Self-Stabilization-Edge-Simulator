package dsslib.events;

/**
 *
 */
public abstract class Event{

    /** Time issued **/
    private long globalTimeIssued;

    private long globalTimeProcessed=-1;

    /** Time processed **/
    private long localTimeProcessed=-1;

    private String info;

    public void setGlobalTimeIssued(long globalTimeIssued) {
        this.globalTimeIssued = globalTimeIssued;
    }

    public long getGlobalTimeIssued() {
        return globalTimeIssued;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public String getInfo() {
        return info;
    }

    public long getLocalTimeProcessed() {
        return localTimeProcessed;
    }

    public void setLocalTimeProcessed(long localTimeProcessed) {
        this.localTimeProcessed = localTimeProcessed;
    }

    @Override
    public final String toString() {
        if(info == null)
            return String.format("%6d\t%6d\t%6d\t%s",globalTimeIssued,localTimeProcessed,globalTimeProcessed,getClass().getTypeName());
        else
            return String.format("%6d\t%6d\t%6d\t%s\t%s", globalTimeIssued,localTimeProcessed,globalTimeProcessed,getClass().getTypeName(),info);
    }

    public long getGlobalTimeProcessed() {
        return globalTimeProcessed;
    }

    public void setGlobalTimeProcessed(long globalTimeProcessed) {
        this.globalTimeProcessed = globalTimeProcessed;
    }

}
