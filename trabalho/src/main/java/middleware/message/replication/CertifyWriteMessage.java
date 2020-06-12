package middleware.message.replication;

import middleware.certifier.OperationalSets;
import middleware.message.Message;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Generic class to represent an operation that requires certification.
 * A certifiable operation needs to hold its WriteSet and Start timestamp.
 * This class also holds state so that it can be applied if the operation is valid.
 */
public class CertifyWriteMessage<V extends Serializable> extends Message
        implements Certifiable, Replicable<V>, Serializable {

    // Maps table name and BitWriteSet
    private final Map<String, OperationalSets> sets;
    private long timestamp;
    private final V state;

    public CertifyWriteMessage(Map<String, OperationalSets> sets, V state){
        this.sets = sets;
        this.state = state;
    }

    @Override
    public Map<String, OperationalSets> getSets() {
        return this.sets;
    }

    @Override
    public V getState() {
        return state;
    }

    @Override
    public long getStartTimestamp() {
        return timestamp;
    }

    @Override
    public Set<String> getTables() { return this.sets.keySet(); }

    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }

}
