package middleware;
import spread.SpreadGroup;

import java.util.*;

/**
 * Class used to check if a server is the leader
 */
public class ElectionManager {

    private Set<SpreadGroup> groupsLeftForLeader;
    private SpreadGroup spreadGroup;

    public ElectionManager(SpreadGroup spreadGroup){
        this.groupsLeftForLeader = new HashSet<>();
        this.spreadGroup = spreadGroup;
    }

    /**
     * Used to know if the current server is the leader after a member left
     * @param oldMember member who left the group
     * @return if the server is the new leader
     */
    public boolean amILeader(SpreadGroup oldMember){
        groupsLeftForLeader.remove(oldMember);
        return this.groupsLeftForLeader.isEmpty();
    }

    /**
     * Used to know if the current server can be the leader after several members left
     * @param updatedMembers current members in the group
     * @return if the server is the new leader
     */
    public boolean amILeader(SpreadGroup[] updatedMembers){
        groupsLeftForLeader.retainAll(Arrays.asList(updatedMembers));
        return this.groupsLeftForLeader.isEmpty();
    }

    /**
     * Used when the server joins a new group
     * @param members members of the group
     */
    public void joinedGroup(SpreadGroup[] members) {
        for (SpreadGroup member:
             members) {
        }
        groupsLeftForLeader.addAll(Arrays.asList(members));
        groupsLeftForLeader.remove(spreadGroup);
    }
}
