package middleware.message.replication;

import middleware.message.Message;

import java.io.Serializable;

public class StateTransferMessage<V> extends Message implements Serializable, Replicable<FullState<V>> {
    private FullState<V> state;

    public StateTransferMessage(FullState<V> state){
        this.state = state;
    }

    @Override
    public FullState<V> getState() {
        return this.state;
    }

}
