package middleware.message.replication;

import middleware.message.Message;

import java.io.Serializable;


//TODO poderá servir para fazer a passagem de estado, quando uma replica fica para trás
/**
 * Class to pass state changes
 */
public class StateTransferMessage<V extends Serializable> extends Message implements Serializable, Replicable<V> {
    private String serverName;
    private V state;

    public StateTransferMessage(String serverName, V state) {
        this.serverName = serverName;
        this.state = state;
    }

    public String getServerName(){
        return serverName;
    }

    public void setState(V state) {
        this.state = state;
    }

    public V getState() {
        return state;
    }
}
