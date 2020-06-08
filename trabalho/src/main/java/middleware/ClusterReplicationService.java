package middleware;

import middleware.certifier.Certifier;
import middleware.certifier.NoTableDefinedException;
import middleware.certifier.WriteSet;
import middleware.logreader.LogReader;
import middleware.message.Message;
import middleware.message.WriteMessage;
import middleware.message.replication.*;
import spread.*;

import java.net.InetAddress;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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
    public  Certifier<K, W> certifier;
    private final ElectionManager electionManager;
    private boolean imLeader;

    private ServerImpl<K, W, ?> server;
    private CompletableFuture<Void> started;
    private LogReader logReader;

    // safe delete
    private ScheduledThreadPoolExecutor executor;

    private SpreadGroup[] members;
    private SpreadGroup[] sdRequested; // SafeDeleteRequest sent to these members
    private List<Long> timestamps;

    public ClusterReplicationService(int spreadPort, String privateName, ServerImpl<K, W, ?> server, int totalServers, LogReader logReader, Connection connection){
        this.totalServers = totalServers;
        this.privateName = privateName;
        this.port = spreadPort;
        this.spreadGroup = new SpreadGroup();
        this.spreadConnection = new SpreadConnection();
        this.server = server;
        this.initializer = new Initializer(server, this, connection);
        //this.cachedMessages = new HashMap<>();
        //TODO recover do estado
        this.certifier = new Certifier<>();
        this.electionManager = new ElectionManager(this.spreadConnection);
        this.imLeader = false;
        this.logReader = logReader;
    }


    public CompletableFuture<Void> start() throws Exception {
        this.spreadConnection.connect(InetAddress.getByName("localhost"), port, this.privateName,
                false, true);
        this.spreadGroup.join(this.spreadConnection, "grupo");
        this.spreadConnection.add(messageListener());
        this.started = new CompletableFuture<>();
        return started;
    }


    public void rebuildCertifier(Certifier c){
        this.certifier = new Certifier<K, W>(c);
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
                floodMessage(msg);
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
        info.getStayed();
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
            System.out.println("Server : " + privateName + ", network partition, im not in main group, will stop working");
        }
    }

    private void handleJoin(MembershipInfo info) throws Exception {
        System.out.println("Server : " + privateName + ", a member joined");
        SpreadGroup newMember = info.getJoined();

        //TODO ENVIAR ESTADO CORRETO. De momento não funciona
        System.out.println("Server : " + privateName + ", I'm leader. Sending state to " + newMember);
        Message message = new GetLengthRequestMessage();
        noAgreementFloodMessage(message, newMember);
    }

    private void handleSelfJoin(MembershipInfo info) {
        if(isInMainPartition(info.getMembers()))
            server.unpause();

        System.out.println("Server : " + privateName + ", I joined");
        SpreadGroup[] members = info.getMembers();
        electionManager.joinedGroup(members);
        // é o primeiro servidor, não precisa de transferência de estado
        if (members.length == 1) {
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





    private void handleWriteMessage(WriteMessage<?> msg) {
        server.handleMessage(msg); // TODO distinguir handlemessage para transaction write e read??
    }


    private void handleCertifyWriteMessage(CertifyWriteMessage<W, ?> cwm) throws NoTableDefinedException {
        System.out.println("Server : " + privateName + " write id: " + cwm.getId() + " message with timestamp: " + cwm.getStartTimestamp());
        boolean isWritable = !certifier.hasConflict(cwm.getWriteSets(), cwm.getStartTimestamp());
        System.out.println("Server : " + privateName + " isWritable: " + isWritable);
        server.handleCertifierAnswer(cwm, isWritable);
    }

    private void handleStateLengthRequestMessage(StateLengthRequestMessage msg, SpreadGroup sender) throws Exception {
        System.out.println("Received request logs");
        int lowerBound = msg.getBody();
        System.out.println("Logs lower bound = " + lowerBound );
        ArrayList<String> queries = new ArrayList<>(logReader.getQueries(lowerBound));
        Message m = new StateTransferMessage(new State(queries, certifier));
        noAgreementFloodMessage(m, sender);
    }



    private void handleSafeDeleteRequestMessage(SpreadGroup sender) throws Exception {
        long ts = certifier.getSafeToDeleteTimestamp();
        SafeDeleteReplyMessage reply = new SafeDeleteReplyMessage(ts);
        sdRequested = members;
        noAgreementFloodMessage(reply, sender);
    }

    private void handleSafeDeleteReplyMessage(SafeDeleteReplyMessage msg) throws Exception {
        long ts = msg.getTs();
        timestamps.add(ts);
        int arrived = timestamps.size();
        if (arrived == sdRequested.length || arrived >= members.length) { // TODO ver se chegaram todos, mas direito
            long minTs = Collections.min(timestamps);
            SafeDeleteMessage sdmsg = new SafeDeleteMessage(minTs);
            floodMessage(sdmsg);
        }
    }

    private void handleSafeDeleteMessage(SafeDeleteMessage msg) {
        long ts = msg.getTs();
        certifier.evictStoredWriteSets(ts);
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
                        if(received instanceof WriteMessage) {
                            handleWriteMessage((WriteMessage<?>) received);

                        } else if(received instanceof CertifyWriteMessage){
                            handleCertifyWriteMessage((CertifyWriteMessage<W, ?>) received);

                        } else if(received instanceof StateLengthRequestMessage){
                            // enviado pelo líder a um membro novo
                            // TODO: Não é Serializable
                            handleStateLengthRequestMessage((StateLengthRequestMessage) received, spreadMessage.getSender());

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


    public LogReader getLogReader() {
        return logReader;
    }
}