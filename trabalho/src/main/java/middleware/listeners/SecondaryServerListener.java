package middleware.listeners;

import middleware.ElectionManager;
import middleware.PassiveReplicationServer;
import middleware.message.Message;
import middleware.message.StateMessage;
import spread.*;

import java.util.LinkedList;
import java.util.Queue;

public class SecondaryServerListener implements AdvancedMessageListener {

    private ElectionManager electionManager;
    private String privateName;

    private PassiveReplicationServer server;
    private Queue<SpreadMessage> messageQueue;
    private Boolean initializing;

    public SecondaryServerListener(SpreadConnection spreadConnection, String privateName,
                                   PassiveReplicationServer server){
        this.server = server;
        this.privateName = privateName;
        this.messageQueue = new LinkedList<>();
        this.initializing = true;
        this.electionManager = new ElectionManager(spreadConnection);
    }


    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        try {

            Message received = (Message) spreadMessage.getObject();
            if(received instanceof StateMessage){
                StateMessage stateMessage = (StateMessage) received;
                if(stateMessage.getServerName().equals(privateName)){
                    initializing = false;
                    messageQueue = null;
                }
            }


            System.out.println("Processing-> " + received);
            if(initializing){
                if(received instanceof StateMessage){
                    initializing = false;
                    for(SpreadMessage sm : messageQueue){
                        server.respondMessage(sm);
                    }
                    messageQueue = null;
                    server.setState(received.getBody());
                }else {
                    messageQueue.add(spreadMessage);
                }
            } else {
                if(!(received instanceof StateMessage)){
                    server.respondMessage(spreadMessage);
                }
            }
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        boolean iAmLeader = electionManager.amIPrimary(spreadMessage);
        if (iAmLeader){
            AdvancedMessageListener primaryListener = new PrimaryServerListener(server);
            server.setMessageListener(primaryListener);
        } else {
            MembershipInfo info = spreadMessage.getMembershipInfo();
            for(SpreadGroup sg : info.getMembers()){
                try {
                    Message message = new StateMessage(server.getState(), privateName);
                    server.floodMessage(message, sg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
