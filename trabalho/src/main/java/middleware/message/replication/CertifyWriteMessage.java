package middleware.message.replication;

import middleware.Certifier.BitWriteSet;
import middleware.message.Message;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
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
    private final Map<String, BitWriteSet> bws;
    private long timestamp;
    private final V state;

    public CertifyWriteMessage(Map<String, BitWriteSet> bws, V state){
        this.bws = bws;
        this.state = state;
    }

    @Override
    public Map<String, BitWriteSet> getWriteSets() {
        return this.bws;
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
    public Set<String> getTables() {
        return this.bws.keySet();
    }

    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }
}
