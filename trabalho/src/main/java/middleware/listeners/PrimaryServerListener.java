package middleware.listeners;

import middleware.PassiveReplicationServer;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.Serializable;

public class PrimaryServerListener implements AdvancedMessageListener {

    private PassiveReplicationServer server;

    public PrimaryServerListener(PassiveReplicationServer server){
        this.server = server;
    }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        // Ideia geral
        //Object received = spreadMessage.getObject(); # è do tipo Message
        //server.handleMessage(received);
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        //server.floodMessage(spreadMessage.getObject());
    }



}
