package servers;

import spread.*;

import java.util.*;

public class ElectionManager {

    private Set<SpreadGroup> groupsLeftForPrimary;
    private boolean isJoinning;
    private SpreadConnection spreadConnection;
    private ElectionHandler electionHandler;

    public ElectionManager(SpreadConnection spreadConnection, ElectionHandler electionHandler){
        this.groupsLeftForPrimary = new HashSet<>();
        this.isJoinning = true;
        this.spreadConnection = spreadConnection;
        this.electionHandler = electionHandler;
    }

    public void receive(SpreadMessage msg){
        List<SpreadGroup> info = Arrays.asList(msg.getMembershipInfo().getMembers());
        if(isJoinning) {
            groupsLeftForPrimary.addAll(info);
            groupsLeftForPrimary.remove(spreadConnection.getPrivateGroup());
            isJoinning = false;
        }

        if(removeSpreadGroups(info) >= 0 && this.groupsLeftForPrimary.isEmpty()){
            electionHandler.elected(msg);
        } else {
            electionHandler.waitingForElection(msg);
        }
    }
    private int removeSpreadGroups(Collection<SpreadGroup> info){
        List<SpreadGroup> toRemove = new LinkedList<>();
        for(SpreadGroup sg : groupsLeftForPrimary){
            if(!info.contains(sg)){
                toRemove.add(sg);
            }
        }
        groupsLeftForPrimary.removeAll(toRemove);
        return toRemove.size();
    }
}
