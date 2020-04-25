package middleware.listeners;

import middleware.PassiveReplicationServer;
import middleware.message.Message;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrimaryServerListener implements AdvancedMessageListener {


    private PassiveReplicationServer server;
    // number of servers on the spread group
    private int nServers;
    // Map<Message.getId(), List<Message>, stores all responses of the other servers by the sent message
    private Map<String, List<Message>> cachedMessages;

    public PrimaryServerListener(PassiveReplicationServer server){
        this.server = server;
        nServers = 1;
        this.cachedMessages = new HashMap<>();
    }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        try{
            Message received = (Message) spreadMessage.getObject();
            cachedMessages.putIfAbsent(received.getId(), new ArrayList<>());
            List<Message> messagesReceived = cachedMessages.get(received.getId());
            messagesReceived.add(received);
            if(messagesReceived.size() == nServers){
                server.respondMessage(spreadMessage);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        MembershipInfo info = spreadMessage.getMembershipInfo();
        nServers = info.getMembers().length;
        for(SpreadGroup sg : info.getMembers()){
            try {
                Message message = new Message(server.getState());
                server.floodMessage(message, sg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
