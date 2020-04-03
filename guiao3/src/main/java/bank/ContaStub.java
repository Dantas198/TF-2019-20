package bank;

import messages.ReplyMessage;
import messages.RequestMessage;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ContaStub {
    private ManagedMessagingService mms;
    private Address primaryServer;
    private Address myAddress;
    private ExecutorService e;
    private Serializer s;
    private int idCount;
    private CompletableFuture<ReplyMessage> res;

    public ContaStub(int myPort){
        this.primaryServer = Address.from(10000);
        this.idCount = 0;
        this.myAddress = Address.from(myPort);
    }

    public void start(){
        this.mms = new NettyMessagingService(
                "server",
                myAddress,
                new MessagingConfig());
        this.mms.start();
        this.s = new SerializerBuilder()
                .addType(ReplyMessage.class)
                .addType(RequestMessage.class)
                .build();
        this.e = Executors.newFixedThreadPool(1);

        this.mms.registerHandler("reply", (a,b) -> {
            ReplyMessage repm = s.decode(b);
            if(repm.reqId == idCount)
                res.complete(repm);
        }, e);
    }

    public int saldo() throws ExecutionException, InterruptedException {
        this.res = new CompletableFuture<>();
        idCount++;
        RequestMessage reqm = new RequestMessage(idCount);
        mms.sendAsync(primaryServer, "request", s.encode(reqm));
        return res.get().q;
    }

    public boolean mov(int q) throws ExecutionException, InterruptedException {
        this.res = new CompletableFuture<>();
        idCount++;
        RequestMessage reqm = new RequestMessage(idCount,q);
        mms.sendAsync(primaryServer, "request", s.encode(reqm));
        return res.get().b;
    }
}
