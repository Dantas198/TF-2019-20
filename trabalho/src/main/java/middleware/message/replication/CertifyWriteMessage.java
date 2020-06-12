package middleware.message.replication;

import middleware.certifier.OperationSet;
import middleware.message.Message;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

/**
 * Generic class to represent an operation that requires certification.
 * A certifiable operation needs to hold its WriteSet and Start timestamp.
 * This class also holds state so that it can be applied if the operation is valid.
 */
public class CertifyWriteMessage<K extends OperationSet<?>, V extends Serializable> extends Message
        implements Certifiable<K>, Replicable<V>, Serializable {

    // Maps table name and BitWriteSet
    private final Map<String, K> bws;
    private final Map<String, K> brs;
    private long timestamp;
    private final V state;

    public CertifyWriteMessage(Map<String, K> bws, Map<String, K> brs, V state){
        this.bws = bws;
        this.brs = brs;
        this.state = state;
    }

    @Override
    public Map<String, K> getWriteSets() {
        return this.bws;
    }

    @Override
    public Map<String, K> getReadSets() {
        return this.brs;
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
    public Set<String> getWriteTables() { return this.bws.keySet(); }

    @Override
    public Set<String> getReadTables() { return this.brs.keySet(); }

    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }

}
