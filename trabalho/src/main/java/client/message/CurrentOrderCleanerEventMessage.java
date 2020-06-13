package client.message;

import middleware.GlobalEvent;
import middleware.message.replication.GlobalEventMessage;

public class CurrentOrderCleanerEventMessage extends GlobalEventMessage {
    private final long time;

    public CurrentOrderCleanerEventMessage(GlobalEvent e, long time) {
        super(e);
        this.time = time;
    }

    public long getTime() {
        return time;
    }
}
