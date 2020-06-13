package middleware;
import middleware.message.LeaderProposal;
import spread.SpreadGroup;

import java.util.*;

/**
 * Class used to check if a server is the leader
 */
public class ElectionManager {

    private Set<SpreadGroup> groupsLeftForLeader;
    private SpreadGroup spreadGroup;
    private SpreadGroup privateGroup;

    //caso tenha de haver troca de grupos com o lider efetivo
    private Set<SpreadGroup> possibleSpreadGroup;
    private SpreadGroup mainCandidate;
    private Long mainCandidateCommitedTimestamp;
    private boolean electionTerminated;

    public ElectionManager(SpreadGroup spreadGroup, SpreadGroup principalCandidate){
        this.groupsLeftForLeader = new HashSet<>();
        this.spreadGroup = spreadGroup;
        this.possibleSpreadGroup = new HashSet<>();
        this.privateGroup = principalCandidate;
        this.mainCandidate = principalCandidate;
        this.electionTerminated = false;
    }


    public void elect(){
        this.electionTerminated = true;
        //se sou o líder não esperarei por mais ninguém
        if(this.mainCandidate.equals(this.privateGroup)){
            groupsLeftForLeader.clear();
        }
        //se sou o oldest candidate "troco" de lugar com o lider efetivo.
        else if(groupsLeftForLeader.isEmpty()){
            this.groupsLeftForLeader = this.possibleSpreadGroup;
            this.possibleSpreadGroup.clear();
        }
    }

    // caso o timestamp seja maior a nova proposta passa a ser o candidato principal
    public void handleLeaderProposal(LeaderProposal lp, SpreadGroup candidate){
        if (lp.getTs() > this.mainCandidateCommitedTimestamp ||
                (lp.getTs().equals(this.mainCandidateCommitedTimestamp)
                        && candidate.toString().compareTo(privateGroup.toString()) > 0)){

            this.mainCandidate = candidate;
            this.possibleSpreadGroup = lp.getGroupsLeftForLeader();
            this.mainCandidateCommitedTimestamp = lp.getTs();
        }
    }

    public boolean isElectionTerminated() {
        return electionTerminated;
    }

    public SpreadGroup getMainCandidate(){
        return this.mainCandidate;
    }

    /**
     * Used to know if the current server is the leader after a member left
     * @param oldMember member who left the group
     * @return if the server is the new leader
     */
    public boolean amILeader(SpreadGroup oldMember){
        groupsLeftForLeader.remove(oldMember);
        return this.groupsLeftForLeader.isEmpty() && electionTerminated;
    }

    /**
     * Used to know if the current server can be the leader after several members left
     * @param updatedMembers current members in the group
     * @return if the server is the new leader
     */
    public boolean amILeader(SpreadGroup[] updatedMembers){
        groupsLeftForLeader.retainAll(Arrays.asList(updatedMembers));
        return this.groupsLeftForLeader.isEmpty() && electionTerminated;
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

    public Set<SpreadGroup> getGroupsLeftForLeader() {
        return groupsLeftForLeader;
    }
}
