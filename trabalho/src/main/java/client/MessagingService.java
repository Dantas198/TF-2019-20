package client;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import middleware.message.ContentMessage;
import middleware.message.ErrorMessage;
import middleware.message.Message;

import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class MessagingService {

    private ManagedMessagingService mms;
    private List<Address> servers;
    private Serializer s;
    private CompletableFuture<Message> res;
    private ScheduledExecutorService ses;


    public MessagingService(int myPort, List<Address> servers){
        this.res = new CompletableFuture<>();
        this.servers = servers;
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
                ContentMessage<?> repm = s.decode(b);
                res.complete(repm);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ses);
    }

    // escalonamento
    private Address chooseServer() {
        Random rand = new Random();
        return servers.get(rand.nextInt(servers.size()));
    }


    private ScheduledFuture<?> scheduleTimeout(Message reqm){
        return ses.scheduleAtFixedRate(()->{
            System.out.println("timeout...sending new request");
            mms.sendAsync(chooseServer(), "request", s.encode(reqm));
        }, 1, 4, TimeUnit.SECONDS);
    }

    public<R extends Message> R sendAndReceive(Message request) throws ExecutionException, InterruptedException {
        return new Request<R>().sendAndReceive(request);
    }

    @SuppressWarnings("unchecked")
    private class Request<R extends Message> {

        // TODO timeout, enviar a outro servidor?
        //  implementar primeiro no servidor para o caso de falhar antes de enviar resposta
        public R sendAndReceive(Message request) throws ExecutionException, InterruptedException {
            res = new CompletableFuture<>();
            /*
            ScheduledFuture<?> sf = scheduleTimeout(reqm);
            //Caso a resposta tenha chegado cancela o timeout
            res.whenComplete((m,t) -> {
                if(t != null)
                    t.printStackTrace();
                sf.cancel(true);
            });
            */
            mms.sendAsync(chooseServer(), "request", s.encode(request));
            return (R) res.thenApply(cm -> {
                System.out.println("Received message: "+ cm);
                if(cm instanceof ErrorMessage)
                    throw new CompletionException(((ErrorMessage) cm).getBody());
                return cm;
            }).get();
        };
    }

}
