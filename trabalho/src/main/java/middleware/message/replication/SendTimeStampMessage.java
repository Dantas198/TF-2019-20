package middleware.message.replication;

import middleware.message.ContentMessage;
import middleware.message.LeaderProposal;

public class SendTimeStampMessage extends ContentMessage<LeaderProposal> {

    public SendTimeStampMessage(LeaderProposal body) {
        super(body);
    }
}
