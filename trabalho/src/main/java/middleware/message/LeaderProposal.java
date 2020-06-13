package middleware.message;

import spread.SpreadGroup;

import java.io.Serializable;
import java.util.Set;

public class LeaderProposal implements Serializable {
    private Long ts;
    private Set<SpreadGroup> groupsLeftForLeader;

    public LeaderProposal(Long ts, Set<SpreadGroup> groupsLeftForLeader){
        this.ts = ts;
        this.groupsLeftForLeader = groupsLeftForLeader;
    }

    public Long getTs() {
        return ts;
    }

    public Set<SpreadGroup> getGroupsLeftForLeader() {
        return groupsLeftForLeader;
    }
}
