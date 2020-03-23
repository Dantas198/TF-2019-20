
import spread.SpreadConnection;
import spread.SpreadException;
import spread.SpreadMessage;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantLock;

public class ContaStub implements Conta {
    private int idCount;
    private SpreadConnection connection;
    private CompletableFuture<ReplyMessage> res;
    private ReentrantLock l;

    public ContaStub(String name) throws UnknownHostException, SpreadException {
        this.idCount = 0;
        l = new ReentrantLock();
        connection = new SpreadConnection();
        connection.connect(InetAddress.getByName("localhost"), 4803, name,
                false, false);

        connection.add(message -> {
            try {
                ReplyMessage repm = (ReplyMessage) message.getObject();
                l.lock();
                if(repm.reqId == idCount)
                    res.complete(repm);
                l.unlock();
            } catch (SpreadException e) {
                e.printStackTrace();
            }
        });
    }

    private void sendMsg(RequestMessage reqm){
        SpreadMessage m = new SpreadMessage();
        m.addGroup("bank");
        try {
            m.setObject(reqm);
            m.setSafe();
            connection.multicast(m);
        } catch (SpreadException e) {
            e.printStackTrace();
        }
    }


    public int saldo() throws ExecutionException, InterruptedException {
        this.res = new CompletableFuture<>();
        l.lock();
        idCount++;
        l.unlock();
        RequestMessage reqm = new RequestMessage(idCount);
        sendMsg(reqm);
        return res.get().q;
    }

    public boolean mov(int q) throws ExecutionException, InterruptedException {
        this.res = new CompletableFuture<>();
        l.lock();
        idCount++;
        l.unlock();
        RequestMessage reqm = new RequestMessage(idCount,q);
        sendMsg(reqm);
        return res.get().b;
    }
}
