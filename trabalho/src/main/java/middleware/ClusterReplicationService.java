package middleware;

import middleware.certifier.WriteSet;
import middleware.message.Message;
import middleware.message.WriteMessage;
import middleware.message.replication.*;
import spread.*;

import java.net.InetAddress;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public class ClusterReplicationService<K, W extends WriteSet<K>> {
    private final int totalServers;
    private final String privateName;
    private final SpreadConnection spreadConnection;
    private final SpreadGroup spreadGroup;
    private final int port;

    //Caso sejam necessário acks poderá ser utilizada esta estrutura
    //private Map<String, List<Message>> cachedMessages;

    private final Initializer initializer;
    private final ElectionManager electionManager;
    private boolean imLeader;

    private ServerImpl<K,W,?> server;
    private CompletableFuture<Void> started;


    // safe delete
    private ScheduledExecutorService executor;

    private SpreadGroup[] members;
    private List<SpreadGroup> sdRequested; // SafeDeleteRequest sent to these members

    //Leader vars
    private boolean evicting;
    private List<Long> timestamps;
    private List<CompletableFuture<Void>> stateRequests;

    public ClusterReplicationService(int spreadPort, String privateName, ServerImpl<K,W,?> server, int totalServers, Connection connection){
        this.totalServers = totalServers;
        this.privateName = privateName;
        this.port = spreadPort;
        this.spreadGroup = new SpreadGroup();
        this.spreadConnection = new SpreadConnection();
        this.server = server;
        this.initializer = new Initializer(server, this, connection);

        //this.cachedMessages = new HashMap<>();
        //TODO recover do estado

        this.electionManager = new ElectionManager(this.spreadGroup);
        this.imLeader = false;

        this.evicting = false;
        this.timestamps = new ArrayList<>();
        this.stateRequests = new ArrayList<>();
        this.executor =Executors.newScheduledThreadPool(1);
    }


    public CompletableFuture<Void> start() throws Exception {
        this.spreadConnection.connect(InetAddress.getByName("localhost"), port, this.privateName + " " + UUID.randomUUID().toString(),
                false, true);
        this.spreadGroup.join(this.spreadConnection, "grupo");
        this.spreadConnection.add(messageListener());
        this.started = new CompletableFuture<>();
        return started;
    }


    /**
     * Method used to respond to the Sender the message defined in the handleMessage abstract method
     * @param spreadMessage
     */
    //TODO corrigir.
    private final Consumer<SpreadMessage> respondMessage = (spreadMessage) -> {
        try {
            Message received = (Message) spreadMessage.getObject();
            Message response = server.handleMessage(received).from(received);
            noAgreementFloodMessage(response, spreadMessage.getSender());
        } catch (Exception e) {
            e.printStackTrace();
        }
    };


    private class SafeDeleteEvent implements Runnable {
        @Override
        public void run() {
            if (!imLeader) return;
            SafeDeleteRequestMessage msg = new SafeDeleteRequestMessage();
            try {
                evicting = true;
                noAgreementFloodMessage(msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void scheduleSafeDeleteEvent() {
        long minutesUntilSafeDelete = 1000;
        executor.schedule(new SafeDeleteEvent(), minutesUntilSafeDelete, TimeUnit.MINUTES);
    }

    private boolean isInMainPartition(SpreadGroup[] partition) {
        return partition.length > totalServers/2;
    }


    private void handleNetworkPartition(MembershipInfo info) {
        SpreadGroup[] stayed = info.getStayed(); // usar getMembers?

        if(isInMainPartition(stayed)) {
            System.out.println("Server : " + privateName + ", network partition, im in main group");
            if(!imLeader) {
                imLeader = electionManager.amILeader(stayed);

                if (imLeader){
                    scheduleSafeDeleteEvent();
                    System.out.println("Assuming leader role");
                }
            }
        } else {
            server.pause();
            initializer.reset();
            System.out.println("Server : " + privateName + ", network partition, im not in main group, will stop working");
        }
    }

    private void handleJoin(MembershipInfo info) throws Exception {
        System.out.println("Server : " + privateName + ", a member joined");
        SpreadGroup newMember = info.getJoined();

        //TODO ENVIAR ESTADO CORRETO. De momento não funciona
        System.out.println("Server : " + privateName + ", I'm leader. Sending state to " + newMember);
        //TODO enviar apenas o que interessa do certifier
        Message message = new GetLengthRequestMessage();
        noAgreementFloodMessage(message, newMember);
    }

    private void handleSelfJoin(MembershipInfo info) {
        //TODO esperar pelo initializer?
        if(isInMainPartition(info.getMembers()))
            server.unpause();

        System.out.println("Server : " + privateName + ", I joined");
        SpreadGroup[] members = info.getMembers();
        electionManager.joinedGroup(members);

        if (members.length == 1) {
            // é o primeiro servidor, não precisa de transferência de estado
            System.out.println("Server : " + privateName + ", and I'm the first member");
            initializer.initialized();
            imLeader = true;
            scheduleSafeDeleteEvent();
            if (!started.isDone())
                started.complete(null);
        }
    }


    private void handleDisconnect(MembershipInfo info) {
        SpreadGroup member = info.getDisconnected();
        System.out.println("Server : " + privateName + ", a member disconnected");
        imLeader = electionManager.amILeader(member);
        if (imLeader){
            scheduleSafeDeleteEvent();
            System.out.println("Assuming leader role");
        }
    }


    private void handleLeave(MembershipInfo info) {
        SpreadGroup member = info.getLeft();
        System.out.println("Server : " + privateName + ", a member left");
        imLeader = electionManager.amILeader(member);
        if (imLeader){
            scheduleSafeDeleteEvent();
            System.out.println("Assuming leader role");
        }
    }


    private void handleCertifyWriteMessage(CertifyWriteMessage<W, ?> cwm) {
        System.out.println("Server : " + privateName + " write id: " + cwm.getId() + " message with timestamp: " + cwm.getStartTimestamp());
        server.handleCertifierAnswer(cwm);
    }

    private void handleStateLengthReplyMessage(StateLengthReplyMessage msg, SpreadGroup sender) throws Exception {
        System.out.println("Received request logs");
        ReplicaLatestState rls = msg.getBody();
        System.out.println("Logs lower bound = " + rls.getLowerBound());
        CompletableFuture<Void> response = new CompletableFuture<>();
        response.thenAccept((x) -> {
            try {
                server.getState(rls.getLowerBound(), rls.getLatestTimestamp()).thenAccept((fullState) -> {
                        Message m = new StateTransferMessage<>(fullState);
                        try {
                            noAgreementFloodMessage(m, sender);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        if (this.evicting)
            this.stateRequests.add(response);
        else
            response.complete(null);
    }

    private void handleSafeDeleteRequestMessage(SpreadGroup sender) {
        server.getSafeToDeleteTimestamp()
            .thenAccept((ts) -> {
                SafeDeleteReplyMessage reply = new SafeDeleteReplyMessage(ts);
                sdRequested = Arrays.asList(members);
                try {
                    noAgreementFloodMessage(reply, sender);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
    }

    private void handleSafeDeleteReplyMessage(SafeDeleteReplyMessage msg) throws Exception {
        long ts = msg.getTs();
        timestamps.add(ts);
        int arrived = timestamps.size();
        if (arrived == sdRequested.size()) { // TODO ver se chegaram todos, mas direito
            long minTs = Collections.min(timestamps);
            SafeDeleteMessage sdmsg = new SafeDeleteMessage(minTs);
            floodMessage(sdmsg);
        }
    }

    private void handleSafeDeleteMessage(SafeDeleteMessage msg) {
        long ts = msg.getTs();
        server.evictStoredWriteSets(ts).thenAccept((x) -> {
            evicting = false;
            this.stateRequests.forEach(req -> req.complete(null));
            this.stateRequests.clear();
        });
    }


    public AdvancedMessageListener messageListener() {
        return new AdvancedMessageListener() {
            @Override
            public void regularMessageReceived(SpreadMessage spreadMessage) {
                try {
                    System.out.println("Server : " + privateName + ", Regular Message received");
                    if (!initializer.isInitializing(spreadMessage, respondMessage)) {
                        if(!started.isDone())
                            started.complete(null);

                        Message received = (Message) spreadMessage.getObject();
                        if(received instanceof CertifyWriteMessage){
                            handleCertifyWriteMessage((CertifyWriteMessage<W, ?>) received);

                        } else if(received instanceof StateLengthReplyMessage){
                            // enviado pelo líder a um membro novo
                            // TODO: Não é Serializable
                            handleStateLengthReplyMessage((StateLengthReplyMessage) received, spreadMessage.getSender());

                        } else if(received instanceof SafeDeleteRequestMessage){
                            // enviado pelo líder a todos os membros no evento de GarbageCollection do certifier
                            handleSafeDeleteRequestMessage(spreadMessage.getSender());

                        } else if(received instanceof SafeDeleteReplyMessage && imLeader) {
                            // resposta á SafeDeleteRequestMessage
                            handleSafeDeleteReplyMessage((SafeDeleteReplyMessage) received);

                        } else if (received instanceof SafeDeleteMessage) {
                            // enviada pelo líder depois de obter as respostas ao SafeDeleteRequest
                            handleSafeDeleteMessage((SafeDeleteMessage) received);
                        }
                        /*
                        cachedMessages.putIfAbsent(received.getId(), new ArrayList<>());
                        List<Message> messagesReceived = cachedMessages.get(received.getId());
                        // se for escrita terá de vir o estado já calculado para ser genérico
                        Message myResponse = server.handleMessage(received).from(received);
                        messagesReceived.add(myResponse);
                        if (messagesReceived.size() >= nServers) {
                            pendingMessages.get(received.getId()).complete(myResponse);
                        }
                        */
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void membershipMessageReceived(SpreadMessage spreadMessage) {
                try {
                    System.out.println("Server : " + privateName + ", Membership Message received -------------");
                    MembershipInfo info = spreadMessage.getMembershipInfo();

                    if(info.isRegularMembership()) {
                        members = info.getMembers();
                    } else return;

                    if(info.isCausedByJoin() && info.getJoined().equals(spreadConnection.getPrivateGroup())) {
                        handleSelfJoin(info);
                        return;
                    }

                    if(info.isCausedByNetwork()) {
                        handleNetworkPartition(info);
                        return;
                    }

                    if(imLeader) {
                        if(info.isCausedByJoin()) {
                            handleJoin(info);
                        }
                        else
                            reduceWaits(info);
                    }
                    else {
                        if(info.isCausedByDisconnect()) {
                            handleDisconnect(info);
                        } else
                        if(info.isCausedByLeave()) {
                            handleLeave(info);
                        }
                    }
                    System.out.println("Server : " + privateName + ", ---------------------------------------");
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
    }

    private void reduceWaits(MembershipInfo info){
        if(evicting){
            SpreadGroup member;
            if(info.isCausedByDisconnect())
                member = info.getDisconnected();
            else
                member = info.getLeft();
            sdRequested.remove(member);
        }
    }




    public void floodMessage(Message msg) throws Exception {
        safeFloodMessage(msg, this.spreadGroup);
    }

    //safe ou agreed?
    public void safeFloodMessage(Message message, SpreadGroup sg) throws Exception{
        SpreadMessage m = new SpreadMessage();
        m.addGroup(sg);
        m.setObject(message);
        m.setSafe();
        spreadConnection.multicast(m);
        System.out.println("Safe flooding to group ("+ this.spreadGroup+ "): ");
    }

    /**
     * Used to respond to all Servers in the current spread group
     * @param message the body message that should be passed to all Servers
     * @throws Exception
     */
    @Deprecated
    public void noAgreementFloodMessage(Message message) throws Exception{
        noAgreementFloodMessage(message, this.spreadGroup);
    }

    /**
     * Used to respond to all Servers connected in the corresponding spread group
     * @param message
     * @param sg
     * @throws Exception
     */
    public void noAgreementFloodMessage(Message message, SpreadGroup sg) throws Exception{
        SpreadMessage m = new SpreadMessage();
        m.addGroup(sg);
        m.setObject(message);
        m.setReliable();
        spreadConnection.multicast(m);
        //System.out.println("Sending to group ("+ sg + "): " + message);
    }

    public void stop() throws SpreadException {
        this.spreadConnection.disconnect();
    }

}