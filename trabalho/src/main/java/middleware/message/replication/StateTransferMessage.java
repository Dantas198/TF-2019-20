package middleware.message.replication;

import middleware.message.Message;

import java.io.Serializable;


//TODO poderá servir para fazer a passagem de estado, quando uma replica fica para trás
/**
 * Class to pass state changes
 */
public class StateTransferMessage<V extends Serializable> extends Message implements Serializable, Replicable<V> {
    private V state;

    public StateTransferMessage(V state) {
        this.state = state;
    }


    public void setState(V state) {
        this.state = state;
    }

    public V getState() {
        return state;
    }
}
