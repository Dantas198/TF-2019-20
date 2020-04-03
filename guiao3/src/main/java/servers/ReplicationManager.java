package servers;

import messages.Acknowledgment;
import messages.TransferStateMessage;
import spread.*;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class ReplicationManager {
    private int numServers;
    //Apenas 1 cliente
    private ReentrantLock l;
    private HashMap<Integer, ReplicationState> replicationByRequestId;

    public ReplicationManager(int numServers) {
        this.numServers = numServers;
        this.l = new ReentrantLock();
        this.replicationByRequestId = new HashMap<>();
    }

    public void createEntry(int id, Consumer<Void> callback){
        l.lock();
        ReplicationState rs = new ReplicationState(id, callback);
        replicationByRequestId.put(id, rs);
        l.unlock();
    }

    public void receiveRegular(SpreadMessage spreadMessage) {
        try {
            Object o = spreadMessage.getObject();
            //Se é uma transferStateMessage então estou a receber a minha própria menságem (eu sou o primário)
            ReplicationState rs;
            if (o instanceof TransferStateMessage) {
                TransferStateMessage ts = (TransferStateMessage) o;
                l.lock();
                rs = replicationByRequestId.get(ts.lastReply.reqId);
                l.unlock();
                rs.primaryConfirmed();
                rs.incrAcks();
                System.out.println("Primary confirmation arrived");
            }
            else{
                Acknowledgment ack = (Acknowledgment) o;
                l.lock();
                rs = replicationByRequestId.get(ack.requestId);
                l.unlock();
                rs.incrAcks();
            }
            if (rs.numAcks == numServers) {
                System.out.println("Secundary confirmation arrived for request: " + rs.requestId);
                l.lock();
                replicationByRequestId.remove(rs.requestId);
                l.unlock();
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
        l.lock();
        for(ReplicationState rs : replicationByRequestId.values()){
            if(rs.primaryConfirmation){
                rs.callback.accept(null);
            }
            replicationByRequestId.remove(rs.requestId);
        }
        l.unlock();
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
