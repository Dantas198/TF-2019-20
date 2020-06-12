package middleware.message.replication;

import middleware.GlobalEvent;
import middleware.message.ContentMessage;

public class GlobalEventMessage extends ContentMessage<GlobalEvent> {
    public GlobalEventMessage(GlobalEvent e){
        super(e);
    }
}
