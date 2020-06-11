package middleware.message.replication;

import middleware.certifier.WriteSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class FullState<V> implements Serializable {
    private ArrayList<String> businessState;
    private HashMap<String, HashMap<Long, WriteSet<V>>> certifierState;


    public FullState(ArrayList<String> businessState, HashMap<String, HashMap<Long, WriteSet<V>>>  certifierState){
        this.businessState = businessState;
        this.certifierState = certifierState;
    }

    public ArrayList<String> getBusinessState() {
        return businessState;
    }

    public HashMap<String, HashMap<Long, WriteSet<V>>>  getCertifierState() {
        return certifierState;
    }
}
