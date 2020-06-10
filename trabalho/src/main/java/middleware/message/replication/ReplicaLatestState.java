package middleware.message.replication;

import java.io.Serializable;

public class ReplicaLatestState implements Serializable {
    private long latestTimestamp;
    private int lowerBound;

    public ReplicaLatestState(long latestTimestamp, int lowerBound){
        this.latestTimestamp= latestTimestamp;
        this.lowerBound = lowerBound;
    }

    public long getLatestTimestamp() {
        return latestTimestamp;
    }

    public int getLowerBound() {
        return lowerBound;
    }
}
