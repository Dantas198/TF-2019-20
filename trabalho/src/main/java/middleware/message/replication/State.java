package middleware.message.replication;

import middleware.certifier.Certifier;
import java.io.Serializable;
import java.util.ArrayList;

public class State implements Serializable {
    private ArrayList<String> businessState;
    private Certifier certifierState;


    public State(ArrayList<String> businessState, Certifier certifierState){
        this.businessState = businessState;
        this.certifierState = certifierState;
    }

    public ArrayList<String> getBusinessState() {
        return businessState;
    }

    public Certifier getCertifierState() {
        return certifierState;
    }
}
