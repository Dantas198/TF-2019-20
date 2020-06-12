package middleware.message.replication;

import middleware.certifier.OperationalSets;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

public class FullState implements Serializable {
    private ArrayList<String> businessState;
    private HashMap<String, HashMap<Long, OperationalSets>> certifierState;


    public FullState(ArrayList<String> businessState, HashMap<String, HashMap<Long, OperationalSets>>  certifierState){
        this.businessState = businessState;
        this.certifierState = certifierState;
    }

    public ArrayList<String> getBusinessState() {
        return businessState;
    }

    public HashMap<String, HashMap<Long, OperationalSets>>  getCertifierState() {
        return certifierState;
    }
}
