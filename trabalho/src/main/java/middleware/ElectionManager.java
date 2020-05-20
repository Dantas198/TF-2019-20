package middleware;
import spread.MembershipInfo;
import spread.SpreadConnection;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.*;

/**
 * Class responsible to notify when a server if it is a secondary server or a primary server
 */

public class ElectionManager {
    private Set<SpreadGroup> groupsLeftForPrimary;
    private SpreadConnection spreadConnection;

    public ElectionManager(SpreadConnection spreadConnection){
        this.groupsLeftForPrimary = new HashSet<>();
        this.spreadConnection = spreadConnection;
    }

    /**
     * Used to know if the current server can be the leader or not
     * @param oldMember The spread message received by a membership message handler
     * @return if the current message allows the server to be the Leader or not
     */
    public boolean amILeader(SpreadGroup oldMember){
        groupsLeftForPrimary.remove(oldMember);
        return this.groupsLeftForPrimary.isEmpty();
    }

    public boolean amILeader(SpreadGroup[] updatedMembers){
        groupsLeftForPrimary.retainAll(Arrays.asList(updatedMembers));
        return this.groupsLeftForPrimary.isEmpty();
    }


    public void joinedGroup(SpreadGroup[] members) {
        groupsLeftForPrimary.addAll(Arrays.asList(members));
        groupsLeftForPrimary.remove(spreadConnection.getPrivateGroup());
    }
}
