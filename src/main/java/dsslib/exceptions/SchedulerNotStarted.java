package dsslib.exceptions;

public class SchedulerNotStarted extends Exception{

    public SchedulerNotStarted(){
        super("Simulation not started. Make sure you called start() method");}

}
