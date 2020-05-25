package middleware;

import middleware.Certifier.Certifier;
import middleware.message.Message;
import middleware.message.replication.CertifyWriteMessage;
import middleware.message.replication.StateTransferMessage;
import spread.*;

import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ClusterReplicationService {
    private final int totalServers;
    private final String privateName;
    private final SpreadConnection spreadConnection;
    private final SpreadGroup spreadGroup;
    private final int port;

    //Caso sejam necessário acks poderá ser utilizada esta estrutura
    //private Map<String, List<Message>> cachedMessages;

    private final Initializer initializer;
    public final Certifier certifier;
    private final ElectionManager electionManager;
    private boolean imLeader;

    private ServerImpl server;
    private CompletableFuture<Void> started;

    public ClusterReplicationService(int spreadPort, String privateName, ServerImpl server, int totalServers){
        this.totalServers = totalServers;
        this.privateName = privateName;
        this.port = spreadPort;
        this.spreadGroup = new SpreadGroup();
        this.spreadConnection = new SpreadConnection();
        this.server = server;
        this.initializer = new Initializer(server);
        //this.cachedMessages = new HashMap<>();
        //TODO recover do estado
        this.certifier = new Certifier();
        this.electionManager = new ElectionManager(this.spreadConnection);
        this.imLeader = false;
    }

    public CompletableFuture<Void> start() throws Exception {
        this.spreadConnection.connect(InetAddress.getByName("localhost"), port, this.privateName,
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

    private void handleNetworkPartition(MembershipInfo info) {
        SpreadGroup[] stayed = info.getStayed(); // usar getMembers?

        // está no grupo maioritário
        if(stayed.length > totalServers/2) {
            System.out.println("Server : " + privateName + ", network partition, im in main group");
            if(!imLeader) {
                imLeader = electionManager.amILeader(stayed);
                if (imLeader){
                    System.out.println("Assuming leader role");
                }
            }
        } else {
            System.out.println("Server : " + privateName + ", network partition, im not in main group, will stop working");
            // TODO parar
        }
    }

    private void handleJoin(MembershipInfo info) throws Exception {
        System.out.println("Server : " + privateName + ", a member joined");
        SpreadGroup newMember = info.getJoined();

        //TODO ENVIAR ESTADO CORRETO. De momento não funciona
        System.out.println("Server : " + privateName + ", I'm leader. Sending state to " + newMember);
        Message message = new StateTransferMessage<>(1);
        noAgreementFloodMessage(message, newMember);
    }

    private void handleSelfJoin(MembershipInfo info) {
        System.out.println("Server : " + privateName + ", I joined");
        SpreadGroup[] members = info.getMembers();
        electionManager.joinedGroup(members);
        // é o primeiro servidor, não precisa de transferência de estado
        if (members.length == 1) {
            System.out.println("Server : " + privateName + ", and I'm the first member");
            initializer.initialized();
            if (!started.isDone())
                started.complete(null);
        }
    }

    private void handleDisconnect(MembershipInfo info) {
        SpreadGroup member = info.getDisconnected();
        System.out.println("Server : " + privateName + ", a member disconnected");
        imLeader = electionManager.amILeader(member);
        if (imLeader){
            System.out.println("Assuming leader role");
        }
    }

    private void handleLeave(MembershipInfo info) {
        SpreadGroup member = info.getLeft();
        System.out.println("Server : " + privateName + ", a member left");
        imLeader = electionManager.amILeader(member);
        if (imLeader){
            System.out.println("Assuming leader role");
        }
    }


    public AdvancedMessageListener messageListener() {
        return new AdvancedMessageListener() {
            @Override
            public void regularMessageReceived(SpreadMessage spreadMessage) {
                try {
                    System.out.println("Server : " + privateName + ", RegularpMessageReceived");
                    if (!initializer.isInitializing(spreadMessage, respondMessage)) {
                        if(!started.isDone())
                            started.complete(null);
                        Message received = (Message) spreadMessage.getObject();
                        if (received instanceof CertifyWriteMessage){
                            CertifyWriteMessage<?> cwm = (CertifyWriteMessage<?>) received;
                            System.out.println("Server : " + privateName + " write id: " + cwm.getId() + " message with timestamp: " + cwm.getStartTimestamp());
                            boolean isWritable = !certifier.hasConflict(cwm.getWriteSet(),  cwm.getTables(), cwm.getStartTimestamp());
                            System.out.println("Server : " + privateName + " isWritable: " + isWritable);
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
                    System.out.println("Server : " + privateName + ", MembershipMessageReceived -------------");
                    MembershipInfo info = spreadMessage.getMembershipInfo();

                    if(info.isCausedByJoin() && info.getJoined() == spreadGroup) {
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
    @Deprecated
    public void noAgreementFloodMessage(Message message, SpreadGroup sg) throws Exception{
        SpreadMessage m = new SpreadMessage();
        m.addGroup(sg);
        m.setObject(message);
        m.setReliable();
        spreadConnection.multicast(m);
        //System.out.println("Sending to group ("+ sg + "): " + message);
    }

}