package middleware.message.replication;

import middleware.message.Message;

public class SafeDeleteMessage extends Message {
    private long ts;

    public SafeDeleteMessage(long ts) {
        this.ts = ts;
    }

    public long getTs() {
        return ts;
    }
}
