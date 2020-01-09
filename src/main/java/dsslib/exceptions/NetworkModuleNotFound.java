package dsslib.exceptions;

public class NetworkModuleNotFound extends Exception{

    public NetworkModuleNotFound(String uuid) {
        super("NetworkModule doesn't exist. Make sure you've registered this module on process: "+uuid);
    }

}
