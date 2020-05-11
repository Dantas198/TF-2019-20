package business.product;

import business.order.Order;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import middleware.message.ErrorMessage;
import middleware.message.Message;

import java.util.concurrent.*;

public class MessagingService {

    private ManagedMessagingService mms;
    private Address primaryServer;
    private Serializer s;
    private CompletableFuture<Message> res;
    private ScheduledExecutorService ses;


    public MessagingService(int myPort, Address primaryServer){
        this.res = new CompletableFuture<>();
        this.primaryServer = primaryServer;
        Address myAddress = io.atomix.utils.net.Address.from("localhost", myPort);
        this.ses = Executors.newScheduledThreadPool(1);

        this.mms = new NettyMessagingService(
                "server",
                myAddress,
                new MessagingConfig());
        this.mms.start();
        this.s = new SerializerBuilder()
                .withRegistrationRequired(false)
                .build();

        this.mms.registerHandler("reply", (a,b) -> {
            try{
                Message repm = s.decode(b);
                res.complete(repm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ses);
    }

    public Message getResponse(Message reqm) throws Exception {
        res = new CompletableFuture<>();
        ScheduledFuture<?> sf = scheduleTimeout(reqm);
        //Caso a resposta tenha chegado cancela o timeout
        res.whenComplete((m,t) -> { if(t != null) t.printStackTrace(); sf.cancel(true);});
        mms.sendAsync(primaryServer, "request", s.encode(reqm));
        return res.thenApply(cm ->
                {System.out.println("Received message: "+ cm);
                    if(cm instanceof ErrorMessage)
                        throw new CompletionException(((ErrorMessage) cm).getBody());
                    return cm;}
        ).get();
    }

    public ScheduledFuture<?> scheduleTimeout(Message reqm){
        return ses.scheduleAtFixedRate(()->{
            System.out.println("timeout...sending new request");
            mms.sendAsync(primaryServer, "request", s.encode(reqm));
        }, 1, 4, TimeUnit.SECONDS);
    }

}
