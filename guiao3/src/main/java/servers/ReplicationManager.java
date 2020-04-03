package servers;

import messages.TransferStateMessage;
import spread.*;

public class ReplicationManager {
    private int numServers;
    private int numAcks;
    private boolean primaryConfirmation;
    private ReplicationHandler replicationHandler;

    public ReplicationManager(ReplicationHandler replicationHandler) {
        this.numAcks = 0;
        this.primaryConfirmation = false;
        this.replicationHandler = replicationHandler;
    }

    public void receiveRegular(SpreadMessage spreadMessage) {
        try {
            Object o = spreadMessage.getObject();
            numAcks++;
            if (o instanceof TransferStateMessage) {
                primaryConfirmation = true;
                System.out.println("Primary confirmation arrived");
            }
            if (numAcks == numServers) {
                System.out.println("Secundary confirmation arrived");
                numAcks = 0;
                primaryConfirmation = false;
                replicationHandler.answerRequest();
            }
        } catch (SpreadException ex) {
            ex.printStackTrace();
        }
    }

    public void receiveMembershipMessage(SpreadMessage spreadMessage) {
        MembershipInfo minfo = spreadMessage.getMembershipInfo();
        numServers = minfo.getMembers().length;
        System.out.println("View changed to: " + numServers + " elements");
        if (primaryConfirmation) {
            System.out.println("primary had already confirmed");
            numAcks = 0;
            primaryConfirmation = false;
            replicationHandler.answerRequest();
        }
    }

    public AdvancedMessageListener getListener() {
        return new AdvancedMessageListener() {
            @Override
            public void regularMessageReceived(SpreadMessage message) {
                receiveRegular(message);
            }

            @Override
            public void membershipMessageReceived(SpreadMessage message) {
                receiveMembershipMessage(message);
            }
        };
    }
}
