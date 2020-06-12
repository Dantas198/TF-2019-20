package middleware.message.replication;

import middleware.certifier.OperationalSets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class FullState<K extends OperationalSets<?>> implements Serializable {
    private ArrayList<String> businessState;
    private HashMap<String, HashMap<Long, K>> certifierState;


    public FullState(ArrayList<String> businessState, HashMap<String, HashMap<Long, K>>  certifierState){
        this.businessState = businessState;
        this.certifierState = certifierState;
    }

    public ArrayList<String> getBusinessState() {
        return businessState;
    }

    public HashMap<String, HashMap<Long, K>>  getCertifierState() {
        return certifierState;
    }
}
