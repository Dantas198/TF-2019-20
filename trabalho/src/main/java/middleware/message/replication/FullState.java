package middleware.message.replication;

import middleware.certifier.OperationSet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class FullState<V> implements Serializable {
    private ArrayList<String> businessState;
    private HashMap<String, HashMap<Long, OperationSet<V>>> certifierState;


    public FullState(ArrayList<String> businessState, HashMap<String, HashMap<Long, OperationSet<V>>>  certifierState){
        this.businessState = businessState;
        this.certifierState = certifierState;
    }

    public ArrayList<String> getBusinessState() {
        return businessState;
    }

    public HashMap<String, HashMap<Long, OperationSet<V>>>  getCertifierState() {
        return certifierState;
    }
}
