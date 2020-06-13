package middleware.message.replication;

import middleware.message.ContentMessage;
import middleware.message.LeaderProposal;

public class GetTimeStampMessage extends ContentMessage<LeaderProposal> {
    public GetTimeStampMessage(LeaderProposal body) {
        super(body);
    }
}
