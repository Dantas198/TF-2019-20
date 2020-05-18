package middleware;

import middleware.Certifier.Certifier;
import middleware.logreader.LogReader;
import middleware.message.Message;
import middleware.message.replication.CertifyWriteMessage;
import middleware.message.replication.StateTransferLengthMessage;
import spread.*;

import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClusterReplicationService {
    private final String privateName;
    private final SpreadConnection spreadConnection;
    private final SpreadGroup spreadGroup;
    private final int port;
    //Caso sejam necessário acks poderá ser utilizada esta estrutura
    //private Map<String, List<Message>> cachedMessages;

    private final Initializor initializor;
    public final Certifier certifier;
    private ServerImpl server;
    private CompletableFuture<Void> started;
    // number of servers on the spread group
    private int nServers;
    private LogReader logReader;
    private Set<SpreadGroup> currentElements;

    public ClusterReplicationService(int spreadPort, String privateName, ServerImpl server){
        this.privateName = privateName;
        this.port = spreadPort;
        this.spreadGroup = new SpreadGroup();
        this.spreadConnection = new SpreadConnection();
        this.server = server;
        this.nServers = 1;
        this.initializor = new Initializor(server);
        //this.cachedMessages = new HashMap<>();
        //TODO recover do estado
        this.certifier = new Certifier();
        this.currentElements = new HashSet<>();
        this.logReader = new LogReader("./" + privateName + ".sql.log");
    }

    public CompletableFuture<Void> start() throws Exception {
        this.spreadConnection.connect(InetAddress.getByName("localhost"), port, this.privateName,
                false, true);
        this.spreadGroup.join(this.spreadConnection, "grupo");
        this.spreadConnection.add(messageListener());
        this.started = new CompletableFuture<>();
        int logSize = logReader.size();
        StateTransferLengthMessage logs = new StateTransferLengthMessage(logSize);
        safeFloodMessage(logs, this.spreadGroup);
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
            floodMessage(response, spreadMessage.getSender());
        } catch (Exception e) {
            e.printStackTrace();
        }
    };

    public AdvancedMessageListener messageListener() {
        return new AdvancedMessageListener() {
            @Override
            public void regularMessageReceived(SpreadMessage spreadMessage) {
                try {
                    if (!initializor.isInitializing(spreadMessage, respondMessage)) {
                        if(!started.isDone())
                            started.complete(null);
                        Message received = (Message) spreadMessage.getObject();
                        if (received instanceof CertifyWriteMessage){
                            CertifyWriteMessage<?> cwm = (CertifyWriteMessage<?>) received;
                            boolean isWritable = certifier.hasConflict(cwm.getWriteSet(), cwm.getStartTimestamp());
                            server.handleCertifierAnswer(cwm, isWritable);
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
                    SpreadGroup[] members = spreadMessage.getMembershipInfo().getMembers();
                    nServers = members.length;
                    if(nServers > currentElements.size()) {
                        if(nServers == 1){
                            initializor.initialized();
                            if(!started.isDone())
                                started.complete(null);
                        }
                        currentElements.add(members[0]);
                        //TODO enviar o estado correto. De momento não funciona
                        Message message = new StateTransferLengthMessage(logReader.size());
                        floodMessage(message, sender);
                    }
                    else{
                        HashSet<SpreadGroup> updatedMembers = new HashSet<>(Arrays.asList(members));
                        SpreadGroup toRemove = null;
                        for(SpreadGroup sg : currentElements){
                            if(!updatedMembers.contains(sg))
                                toRemove = sg;
                        }
                        if(toRemove != null)
                            currentElements.remove(toRemove);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
    }

    public void floodCertifyMessage(CertifyWriteMessage<?> cwm) throws Exception {
        safeFloodMessage(cwm, this.spreadGroup);
    }

    //safe ou agreed?
    public void safeFloodMessage(Message message, SpreadGroup sg) throws Exception{
        SpreadMessage m = new SpreadMessage();
        m.addGroup(sg);
        m.setObject(message);
        m.setSafe();
        spreadConnection.multicast(m);
        System.out.println("Safe flooding to group ("+ this.spreadGroup+ "): " + message);
    }

    /**
     * Used to respond to all Servers in the current spread group
     * @param message the body message that should be passed to all Servers
     * @throws Exception
     */
    @Deprecated
    public void floodMessage(Message message) throws Exception{
        floodMessage(message, this.spreadGroup);
    }

    /**
     * Used to respond to all Servers connected in the corresponding spread group
     * @param message
     * @param sg
     * @throws Exception
     */
    @Deprecated
    public void floodMessage(Message message, SpreadGroup sg) throws Exception{
        SpreadMessage m = new SpreadMessage();
        m.addGroup(sg);
        m.setObject(message);
        m.setReliable();
        spreadConnection.multicast(m);
        System.out.println("Flooding to group ("+ this.spreadGroup+ "): " + message);
    }

}
