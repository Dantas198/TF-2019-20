package bank;

import messages.ReplyMessage;
import messages.RequestMessage;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;

import java.util.concurrent.*;


public class ContaStub {
    private ManagedMessagingService mms;
    private Address primaryServer;
    private Address myAddress;
    private Serializer s;
    private int idCount;
    private CompletableFuture<ReplyMessage> res;
    private ScheduledExecutorService ses;

    public ContaStub(int myPort){
        this.primaryServer = Address.from(10000);
        this.idCount = 0;
        this.myAddress = Address.from(myPort);
        this.ses = Executors.newScheduledThreadPool(1);
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

        this.mms.registerHandler("reply", (a,b) -> {
            ReplyMessage repm = s.decode(b);
            if(repm.reqId == idCount)
                res.complete(repm);
        }, ses);
    }

    public int saldo() throws ExecutionException, InterruptedException {
        this.res = new CompletableFuture<>();
        idCount++;
        RequestMessage reqm = new RequestMessage(idCount);
        //Agenda um timeout
        ScheduledFuture<?> sf = scheduleTimeout(reqm);
        //Caso a resposta tenha chegado cancela o timeout
        res.whenComplete((m,t) -> sf.cancel(true));
        mms.sendAsync(primaryServer, "request", s.encode(reqm));
        return res.get().q;
    }

    public boolean mov(int q) throws ExecutionException, InterruptedException {
        this.res = new CompletableFuture<>();
        idCount++;
        RequestMessage reqm = new RequestMessage(idCount,q);
        ScheduledFuture<?> sf = scheduleTimeout(reqm);
        res.whenComplete((m,t) -> sf.cancel(true));
        mms.sendAsync(primaryServer, "request", s.encode(reqm));
        return res.get().b;
    }

    public ScheduledFuture<?> scheduleTimeout(RequestMessage reqm){
        return ses.scheduleAtFixedRate(()->{
            System.out.println("timeout...sending new request");
            mms.sendAsync(primaryServer, "request", s.encode(reqm));
            }, 1, 4, TimeUnit.SECONDS);
    }
}
