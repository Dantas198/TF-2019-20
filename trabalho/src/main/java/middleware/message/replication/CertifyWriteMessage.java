package middleware.message.replication;

import middleware.Certifier.BitWriteSet;
import middleware.message.Message;

import java.io.Serializable;

/**
 * Generic class to represent an operation that requires certification.
 * A certifiable operation needs to hold its WriteSet and Start timestamp.
 * This class also holds state so that it can be applied if the operation is valid.
 */
public class CertifyWriteMessage<V extends Serializable> extends Message
        implements Certifiable, Replicable<V>, Serializable {

    private final BitWriteSet bws;
    private long timestamp;
    private final V state;

    public CertifyWriteMessage(BitWriteSet bws, V state){
        this.bws = bws;
        this.state = state;
    }

    @Override
    public BitWriteSet getWriteSet() {
        return bws;
    }

    @Override
    public V getState() {
        return state;
    }

    @Override
    public long getStartTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp){
        this.timestamp = timestamp;
    }
}
