package middleware.message;

import spread.SpreadGroup;

import java.io.Serializable;
import java.util.Set;

public class LeaderProposal implements Serializable {
    private Long ts;

    public LeaderProposal(Long ts, Set<SpreadGroup> groupsLeftForLeader){
        this.ts = ts;
    }

    public Long getTs() {
        return ts;
    }

}
