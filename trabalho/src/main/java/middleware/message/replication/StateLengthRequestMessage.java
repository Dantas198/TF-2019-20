package middleware.message.replication;

import middleware.message.ContentMessage;
import middleware.message.Message;

public class StateLengthRequestMessage extends ContentMessage<Integer> {
    public StateLengthRequestMessage(Integer body) {
        super(body);
    }
}
