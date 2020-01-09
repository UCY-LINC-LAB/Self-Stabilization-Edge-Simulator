package dsslib.exceptions;

public class NoRequiredModuleFound extends Exception{

    public NoRequiredModuleFound(String id){
        super("Process "+id+" is has no required module");
    }

}
