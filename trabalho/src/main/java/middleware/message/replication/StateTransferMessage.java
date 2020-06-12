package middleware.message.replication;

import middleware.message.Message;
import java.io.Serializable;

@Deprecated
public class StateTransferMessage extends Message implements Serializable, Replicable<FullState> {
    private FullState state;

    public StateTransferMessage(FullState state){
        this.state = state;
    }

    @Override
    public FullState getState() {
        return this.state;
    }

}
