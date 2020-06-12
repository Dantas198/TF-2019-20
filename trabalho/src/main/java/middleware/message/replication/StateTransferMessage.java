package middleware.message.replication;

import middleware.certifier.OperationalSets;
import middleware.message.Message;
import java.io.Serializable;

@Deprecated
public class StateTransferMessage<K extends OperationalSets<?>> extends Message implements Serializable, Replicable<FullState<K>> {
    private FullState<K> state;

    public StateTransferMessage(FullState<K> state){
        this.state = state;
    }

    @Override
    public FullState<K> getState() {
        return this.state;
    }

}
