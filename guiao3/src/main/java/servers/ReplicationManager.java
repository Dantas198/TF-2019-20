package servers;

import messages.Acknowledgment;
import messages.TransferStateMessage;
import spread.*;

import java.util.HashMap;
import java.util.function.Consumer;

public class ReplicationManager {
    private int numServers;
    //Apenas 1 cliente
    private HashMap<Integer, ReplicationState> replicationByRequestId;

    public ReplicationManager(int numServers) {
        this.numServers = numServers;
        this.replicationByRequestId = new HashMap<>();
    }

    public void createEntry(int id, Consumer<Void> callback){
        ReplicationState rs = new ReplicationState(id, callback);
        replicationByRequestId.put(id, rs);
    }

    public void receiveRegular(SpreadMessage spreadMessage) {
        try {
            Object o = spreadMessage.getObject();
            //Se é uma transferStateMessage então estou a receber a minha própria menságem (eu sou o primário)
            ReplicationState rs;
            if (o instanceof TransferStateMessage) {
                TransferStateMessage ts = (TransferStateMessage) o;
                rs = replicationByRequestId.get(ts.lastReply.reqId);
                rs.primaryConfirmed();
                rs.incrAcks();
                System.out.println("Primary confirmation arrived");
            }
            else{
                Acknowledgment ack = (Acknowledgment) o;
                rs = replicationByRequestId.get(ack.requestId);
                rs.incrAcks();
            }
            if (rs.numAcks == numServers) {
                System.out.println("Secundary confirmation arrived for request: " + rs.requestId);
                replicationByRequestId.remove(rs.requestId);
                rs.callback.accept(null);
            }
        } catch (SpreadException ex) {
            ex.printStackTrace();
        }
    }

    public void receiveMembershipMessage(SpreadMessage spreadMessage) {
        MembershipInfo minfo = spreadMessage.getMembershipInfo();
        numServers = minfo.getMembers().length;
        System.out.println("View changed to: " + numServers + " elements");
        for(ReplicationState rs : replicationByRequestId.values()){
            if(rs.primaryConfirmation){
                rs.callback.accept(null);
            }
            replicationByRequestId.remove(rs.requestId);
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
