package middleware.message.replication;

import middleware.message.Message;

public class SafeDeleteReplyMessage extends Message  {

    private long ts;

    public SafeDeleteReplyMessage(long ts) {
        this.ts = ts;
    }

    public long getTs() {
        return ts;
    }
}
