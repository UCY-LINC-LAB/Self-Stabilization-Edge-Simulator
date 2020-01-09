package dsslib.exceptions;

public class ProcessNotFound extends Exception{

    public ProcessNotFound(String id){
        super("Process "+id+" is not registered");
    }

}
