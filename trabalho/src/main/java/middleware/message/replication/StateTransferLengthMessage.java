package middleware.message.replication;

import middleware.message.ContentMessage;
import middleware.message.Message;

public class StateTransferLengthMessage extends ContentMessage<Integer> {
    public StateTransferLengthMessage(Integer body) {
        super(body);
    }
}
