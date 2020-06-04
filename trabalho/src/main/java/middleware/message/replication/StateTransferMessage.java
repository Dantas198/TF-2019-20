package middleware.message.replication;

import middleware.message.Message;

import java.io.Serializable;

//TODO poderá servir para fazer a passagem de estado, quando uma replica fica para trás
/**
 * Class to pass state changes
 */
public class StateTransferMessage extends Message implements Serializable, Replicable<State> {
    private State state;

    public StateTransferMessage(State state) {
        this.state = state;
    }


    public void setState(State state) {
        this.state = state;
    }

    public State getState() {
        return state;
    }
}
