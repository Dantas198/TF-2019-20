package middleware;

import middleware.message.LeaderProposal;
import middleware.message.Message;
import middleware.message.replication.*;
import middleware.reader.Pair;
import spread.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.*;


public class ClusterReplicationService {
    private final int totalServers;
    private final String privateName;
    private final SpreadConnection spreadConnection;
    private final SpreadGroup spreadGroup;
    private final int port;
    private Connection dbConnection;
    private final Initializer initializer;
    ElectionManager electionManager;
    private boolean imLeader;

    private ServerImpl server;
    private CompletableFuture<Void> started;

    // safe delete
    private ScheduledExecutorService executor;

    private SpreadGroup[] members;
    private List<SpreadGroup> sdRequested; // SafeDeleteRequest sent to these members

    //Leader vars
    private boolean evicting;
    private List<Long> timestamps;
    private List<CompletableFuture<Void>> stateRequests;
    private final List<GlobalEvent> events;

    public ClusterReplicationService(int spreadPort, String privateName, ServerImpl server, int totalServers, Connection connection, List<GlobalEvent> events){
        this.totalServers = totalServers;
        this.privateName = privateName;
        this.port = spreadPort;
        this.spreadGroup = new SpreadGroup();
        this.spreadConnection = new SpreadConnection();
        this.server = server;
        this.initializer = new Initializer(server, this, privateName);
        this.dbConnection = connection;
        this.events = events;
        this.imLeader = false;

        this.evicting = false;
        this.timestamps = new ArrayList<>();
        this.stateRequests = new ArrayList<>();
        this.executor = Executors.newScheduledThreadPool(1);
    }


    public void joinGroup() throws SpreadException, UnknownHostException {
        this.spreadConnection.connect(InetAddress.getByName("localhost"), port, this.privateName,
                false, true);
        this.spreadGroup.join(this.spreadConnection, "grupo");
    }

    public CompletableFuture<Void> start() throws Exception {
        joinGroup();
        this.spreadConnection.add(messageListener());
        this.started = new CompletableFuture<>();
        return started;
    }

    public void stop() throws SpreadException {
        this.spreadConnection.disconnect();
    }

    public Connection getDbConnection() {
        return dbConnection;
    }

    protected boolean isInMainPartition() {
        return this.members.length > totalServers/2;
    }

    protected SpreadGroup getPrivateSpreadGroup(){
        return this.spreadConnection.getPrivateGroup();
    }

    private void requestLeaderState(SpreadGroup leader){
        Message message = null;
        try {
            message = new SendTimeStampMessage(new LeaderProposal(server.getTimestamp(), electionManager.getGroupsLeftForLeader()));
            noAgreementFloodMessage(message, leader);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ###################################################################
    // Messaging Utilities
    // ###################################################################

    public Set<SpreadGroup> getGroupsLeftForLeader(){
        return this.electionManager.getGroupsLeftForLeader();
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



    // ###################################################################
    // Listener
    // ###################################################################


    public AdvancedMessageListener messageListener() {
        return new AdvancedMessageListener() {
            @Override
            public void regularMessageReceived(SpreadMessage spreadMessage) {
                try {
                    if (!initializer.isInitializing(spreadMessage)) {
                        if(!started.isDone())
                            started.complete(null);

                        if(isInMainPartition()) // no caso de estar em pausa por causa de partições
                            server.unpause();

                        Message received = (Message) spreadMessage.getObject();
                        if(received instanceof CertifyWriteMessage){
                            handleCertifyWriteMessage((CertifyWriteMessage<?>) received);

                        } else if(received instanceof SafeDeleteRequestMessage){
                            // enviado pelo líder a todos os membros no evento de GarbageCollection do certifier
                            handleSafeDeleteRequestMessage(spreadMessage.getSender());

                        } else if(received instanceof SafeDeleteReplyMessage && imLeader) {
                            // resposta á SafeDeleteRequestMessage
                            handleSafeDeleteReplyMessage((SafeDeleteReplyMessage) received);

                        } else if (received instanceof SafeDeleteMessage) {
                            // enviada pelo líder depois de obter as respostas ao SafeDeleteRequest
                            handleSafeDeleteMessage((SafeDeleteMessage) received);

                        } else if (received instanceof SendTimeStampMessage){
                            // enviada pelo líder depois de receber o timestamp do GetTimeStampMessage
                            handleSendTimeStampMessage((SendTimeStampMessage) received, spreadMessage.getSender());
                        } else if (received instanceof GlobalEventMessage){
                            handleGlobalEventMessage((GlobalEventMessage) received);
                        }
                    }
                //se não iniciou ainda poderá estar à espera da eleição do líder
                else{
                    //já estamos num grupo maioritário e o líder ainda não foi eleito então elege-se
                    if(isInMainPartition() && !electionManager.isElectionTerminated()){
                        SpreadGroup privateGroup = spreadConnection.getPrivateGroup();
                        boolean toSwitch = electionManager.elect();
                        //se sou o líder posso iniciar logo
                        if (electionManager.getMainCandidate().equals(privateGroup)){
                            System.out.println("Becoming leader");
                            imLeader = true;
                            initializer.initialized();
                            scheduleLeaderEvents();
                            if (!started.isDone())
                                started.complete(null);
                        }
                        //não sou lider peço o estado a quem é
                        else{
                            if(toSwitch){
                                joinGroup();
                            }
                            else{
                                System.out.println("Not a leader. Requesting leader state");
                                requestLeaderState(electionManager.getMainCandidate());
                            }
                        }
                    }
                }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void membershipMessageReceived(SpreadMessage spreadMessage) {
                try {
                    MembershipInfo info = spreadMessage.getMembershipInfo();

                    if(!info.isRegularMembership()) {
                        return;
                    }
                    if(info.isCausedByJoin()) {
                        members = info.getMembers();
                        handleJoin(info);
                    }
                    else if(info.isCausedByNetwork()) {
                        handleNetworkPartition(info);
                    }
                    else if(info.isCausedByDisconnect()) {
                        members = info.getMembers();
                        handleDisconnect(info);
                    }
                    else if(info.isCausedByLeave()) {
                        members = info.getMembers();
                        handleLeave(info);
                    }
                } catch(Exception e){
                    e.printStackTrace();
                }
            }
        };
    }


    // ###################################################################
    // Handling MembershipMessage
    // ###################################################################

    private void handleNetworkPartition(MembershipInfo info) {
        System.out.println(privateName + ": MembershipMessage received -> network partition");
        System.out.println(privateName + ": Partition members: " + Arrays.toString(this.members));

        Set<SpreadGroup> oldset = new HashSet<>(Arrays.asList(this.members));
        Set<SpreadGroup> newset = new HashSet<>(Arrays.asList(info.getMembers()));

        oldset.removeAll(newset);

        if(isInMainPartition()) {
            System.out.println(privateName + ": I'm in main partition");
            if(!imLeader) {
                imLeader = electionManager.amILeader(this.members);
                if (imLeader){
                    scheduleLeaderEvents();
                    if(evicting)
                        sdRequested.removeAll(oldset);
                    System.out.println(privateName + ": Assuming leader role");
                }
            }
        } else {
            System.out.println(privateName + ": Not a main partition, pausing server");
            server.pause();
            initializer.reset();
            //caso seja o líder deixa de ser. Na verdade o electionManager leva reset quando entra num grupo
            imLeader = false;
        }
    }

    private void handleJoin(MembershipInfo info) throws Exception{
        SpreadGroup newMember = info.getJoined();
        if(newMember.equals(spreadConnection.getPrivateGroup())) {
            SpreadGroup[] members = info.getMembers();
            this.electionManager = new ElectionManager(this.spreadGroup, spreadConnection.getPrivateGroup(), server.getTimestamp());
            electionManager.joinedGroup(members);
        }else{
            System.out.println(privateName + ": MembershipMessage received -> join");
            System.out.println(privateName + ": Member: " + newMember);
            if(imLeader || !electionManager.isElectionTerminated()) {
                System.out.println(privateName + ": I'm leader. Requesting state diff from " + newMember);
                Message message = new GetTimeStampMessage(new LeaderProposal(server.getTimestamp(), electionManager.getGroupsLeftForLeader()));
                noAgreementFloodMessage(message);
            }
        }
    }


    private void handleDisconnect(MembershipInfo info) {
        SpreadGroup member = info.getDisconnected();
        System.out.println(privateName + ": MembershipMessage received -> disconnect");
        handleDL(member);
    }

    private void handleLeave(MembershipInfo info) {
        SpreadGroup member = info.getLeft();
        System.out.println(privateName + ": MembershipMessage received -> left");
        handleDL(member);
    }

    private void handleDL(SpreadGroup member) {
        if (isInMainPartition()){
            imLeader = electionManager.amILeader(member);
            System.out.println(privateName + ": Member: " + member);
            if (imLeader){
                System.out.println(privateName + ": Assuming leader role");
                scheduleLeaderEvents();
                if(evicting) {
                    sdRequested.remove(member);
                }
            }
        }
        else{
            System.out.println(privateName + ": Not a main partition, pausing server");
            server.pause();
            initializer.reset();
            //caso seja o líder deixa de ser. Na verdade o electionManager leva reset quando entra num grupo
            imLeader = false;
        }
    }


    // ###################################################################
    // Handling RegularMessage
    // ###################################################################

    private void handleCertifyWriteMessage(CertifyWriteMessage<?> cwm) {
        System.out.println(privateName + ": RegularMessage received -> CertifyWrite");
        System.out.println(privateName + ": write id: " + cwm.getId() + " message with timestamp: " + cwm.getStartTimestamp());
        server.handleCertifierAnswer(cwm);
    }

    private void handleSafeDeleteRequestMessage(SpreadGroup sender) {
        System.out.println(privateName + ": RegularMessage received -> SafeDeleteRequest");
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
        System.out.println(privateName + ": RegularMessage received -> SafeDeleteReply");
        long ts = msg.getTs();
        timestamps.add(ts);
        int arrived = timestamps.size();
        if (arrived >= sdRequested.size()) {
            long minTs = Collections.min(timestamps);
            SafeDeleteMessage sdmsg = new SafeDeleteMessage(minTs);
            floodMessage(sdmsg);
        }
    }

    private void handleSafeDeleteMessage(SafeDeleteMessage msg) {
        System.out.println(privateName + ": RegularMessage received -> SafeDelete");
        long ts = msg.getTs();
        server.evictStoredWriteSets(ts).thenAccept((x) -> {
            evicting = false;
            this.stateRequests.forEach(req -> req.complete(null));
            this.stateRequests.clear();
            scheduleSafeDeleteEvent();
        });
    }


    private void handleGlobalEventMessage(GlobalEventMessage msg){
        System.out.println(privateName + ": RegularMessage received -> GlobalEventMessage");
        server.handleGlobalEvent(msg);
        scheduleGlobalEvent(msg.getBody());
    }

    private void handleSendTimeStampMessage(SendTimeStampMessage msg, SpreadGroup sender) throws Exception {
        long timeStamp = msg.getBody().getTs();
        System.out.println("Handling SendTimeStampMessage");
        String script = null;
        long currentLowWaterMark = server.getCertifier().getLowWaterMark();
        long currentTimeStamp = server.getCertifier().getTimestamp();
        ArrayList<Pair<String, Long>> queries = server.getLogReader().getLogsAfter(timeStamp);
        if(queries.size() == 0){
            script = Files.readString(FileSystems.getDefault().getPath("db/" + server.getPrivateName() + ".log"));
        }
        Message response = new DBReplicationMessage(script, queries, currentLowWaterMark, currentTimeStamp);
        noAgreementFloodMessage(response, sender);
    }


    // ###################################################################
    // Safe Delete
    // ###################################################################

    private void scheduleLeaderEvents(){
        scheduleSafeDeleteEvent();
        events.forEach(this::scheduleGlobalEvent);
    }


    private void scheduleGlobalEvent(GlobalEvent e) {
        executor.schedule(() -> {
            if (!imLeader) return;
            try {
                GlobalEventMessage globalEventMessage = server.createEvent(e);
                if(globalEventMessage != null) {
                    floodMessage(globalEventMessage);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, e.getTime(), TimeUnit.MINUTES);
    }


    private class SafeDeleteEvent implements Runnable {
        @Override
        public void run() {
            if (!imLeader) return;
            SafeDeleteRequestMessage msg = new SafeDeleteRequestMessage();
            try {
                evicting = true;
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




}