package middleware.message.replication;

import middleware.message.ContentMessage;

@Deprecated
public class StateLengthReplyMessage extends ContentMessage<ReplicaLatestState> {
    public StateLengthReplyMessage(ReplicaLatestState body) {
        super(body);
    }
}
