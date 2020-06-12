package middleware.message.replication;

import middleware.message.ContentMessage;

public class SendTimeStampMessage extends ContentMessage<Long> {

    public SendTimeStampMessage(Long body) {
        super(body);
    }
}
