package dsslib.exceptions;

import dsslib.components.IModule;

public class MissingModule extends Exception{

    public MissingModule(Class<? extends IModule> module) {
        super(module.getTypeName()+" is missing. Make sure you've registered this module");
    }

}
