package middleware.message.replication;

import middleware.message.Message;

import java.io.Serializable;
import java.util.ArrayList;


//TODO poderá servir para fazer a passagem de estado, quando uma replica fica para trás
/**
 * Class to pass state changes
 */
public class StateTransferMessage extends Message implements Serializable, Replicable<ArrayList> {
    private ArrayList<String> state;

    public StateTransferMessage(ArrayList<String> state) {
        this.state = state;
    }


    public void setState(ArrayList<String> state) {
        this.state = state;
    }

    public ArrayList<String> getState() {
        return state;
    }
}
