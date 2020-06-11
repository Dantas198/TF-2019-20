package middleware.message.replication;

import middleware.message.ContentMessage;

public class StateLengthReplyMessage extends ContentMessage<ReplicaLatestState> {
    public StateLengthReplyMessage(ReplicaLatestState body) {
        super(body);
    }
}
