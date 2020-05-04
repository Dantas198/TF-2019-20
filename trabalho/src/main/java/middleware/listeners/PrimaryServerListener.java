package middleware.listeners;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import middleware.PassiveReplicationServer;
import middleware.message.ContentMessage;
import middleware.message.Message;
import spread.AdvancedMessageListener;
import spread.MembershipInfo;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrimaryServerListener implements AdvancedMessageListener {


    private PassiveReplicationServer server;
    // number of servers on the spread group
    private int nServers;
    // Map<Message.getId(), List<Message>, stores all responses of the other servers by the sent message
    private Map<String, List<Message>> cachedMessages;
    private Map<String, CompletableFuture<Message>> finishedMessages;
    // ATOMIX
    // Adress to receive requests from client via Atomix
    private Address myAddress;
    private ExecutorService e;
    private Serializer s;
    private ManagedMessagingService mms;

    public PrimaryServerListener(PassiveReplicationServer server, int portAtomix){
        this.server = server;
        this.myAddress = Address.from("localhost", portAtomix);
        nServers = 1;
        this.cachedMessages = new HashMap<>();
        this.finishedMessages = new HashMap<>();
        this.e = Executors.newFixedThreadPool(1);

        System.out.println("Servidor Primário a iniciar");

        this.s = new SerializerBuilder().withRegistrationRequired(false).build();
        this.mms = new NettyMessagingService(
                "server",
                myAddress,
                new MessagingConfig());
        this.mms.start();

        mms.registerHandler("request", (a,b) -> {
            Message reqm = s.decode(b);
            CompletableFuture<Message> res = new CompletableFuture<>();
            finishedMessages.putIfAbsent(reqm.getId(), res);
            try {
                server.floodMessage(reqm);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            res.thenAccept(message -> {
                System.out.println("Sending response message: " + message);
                System.out.println(a);
                mms.sendAsync(a,"reply",s.encode(message)).whenComplete((m,t) -> {
                    if(t != null){
                        t.printStackTrace();
                    }
                });
            });
        },e);
    }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        try{
            Message received = (Message) spreadMessage.getObject();
            cachedMessages.putIfAbsent(received.getId(), new ArrayList<>());
            List<Message> messagesReceived = cachedMessages.get(received.getId());
            System.out.println("Received message with id: "  + received.getId());
            Message myResponse = server.handleMessage(received).from(received);
            System.out.println("Handled message with id: "  + myResponse.getId() + "; " + myResponse);
            messagesReceived.add(myResponse);
            System.out.println("Received " + messagesReceived.size() +" from "+ nServers +": " + received.getId());
            if(messagesReceived.size() >= nServers){
                finishedMessages.get(received.getId()).complete(myResponse);
            }
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void membershipMessageReceived(SpreadMessage spreadMessage) {
        MembershipInfo info = spreadMessage.getMembershipInfo();
        nServers = info.getMembers().length;
        try {
            Message message = new ContentMessage<>(server.getState());
            server.floodMessage(message, spreadMessage.getSender());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
