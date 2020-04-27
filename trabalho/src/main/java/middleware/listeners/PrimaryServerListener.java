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
        this.myAddress = Address.from(portAtomix);
        nServers = 1;
        this.cachedMessages = new HashMap<>();
        this.finishedMessages = new HashMap<>();
        this.e = Executors.newFixedThreadPool(1);
        this.s = new SerializerBuilder().build();
        this.mms = new NettyMessagingService(
                "server",
                myAddress,
                new MessagingConfig());
        this.mms.start();

        mms.registerHandler("request", (a,b) -> {
            Message reqm = s.decode(b);
            CompletableFuture<Message> res = new CompletableFuture<>();
            finishedMessages.putIfAbsent(reqm.getId(), new CompletableFuture<>());
            try {
                server.floodMessage(reqm);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            res.thenAccept(message -> {
                mms.sendAsync(a,"reply",s.encode(message));
            });
        },e);
    }

    @Override
    public void regularMessageReceived(SpreadMessage spreadMessage) {
        try{
            Message received = (Message) spreadMessage.getObject();
            cachedMessages.putIfAbsent(received.getId(), new ArrayList<>());
            List<Message> messagesReceived = cachedMessages.get(received.getId());
            messagesReceived.add(received);
            if(messagesReceived.size() == nServers){
                finishedMessages.get(received.getId()).complete(received);
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
