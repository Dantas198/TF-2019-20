package middleware;
import spread.SpreadConnection;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.*;

/**
 * Class responsible to notify when a server if it is a secondary server or a primary server
 */
@Deprecated  //em discussão
public class ElectionManager {

    // Remaining groups to le elected as primary
    private Set<SpreadGroup> groupsLeftForPrimary;
    // true if its the first time it receives a membership message, false otherwise
    private boolean isJoinning;
    private SpreadConnection spreadConnection;

    public ElectionManager(SpreadConnection spreadConnection){
        this.groupsLeftForPrimary = new HashSet<>();
        this.isJoinning = true;
        this.spreadConnection = spreadConnection;
    }


    /**
     * Used to know if the current server can be the leader or not
     * @param msg The spread message received by a membership message handler
     * @return if the current message allows the server to be the Leader or not
     */
    public boolean amIPrimary(SpreadMessage msg){
        List<SpreadGroup> info = Arrays.asList(msg.getMembershipInfo().getMembers());
        if(isJoinning) {
            groupsLeftForPrimary.addAll(info);
            groupsLeftForPrimary.remove(spreadConnection.getPrivateGroup());
            isJoinning = false;
        }

        return removeSpreadGroups(info) >= 0 && this.groupsLeftForPrimary.isEmpty();
    }

    /**
     * Removes the groups that left the spreadGroup
     * @param info
     * @return
     */
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
