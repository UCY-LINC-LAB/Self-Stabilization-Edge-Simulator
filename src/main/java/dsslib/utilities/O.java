package dsslib.utilities;

import java.util.HashMap;

public class O extends HashMap<String,Object> {

    public static O create(String k,Object v){
        return new O(k,v);
    }

    public O(){
        super();
    }
    public O(String k, Object v){
        this.put(k,v);
    }
    public O add(String k, Object v){
        this.put(k,v);
        return this;
    }

}
