package middleware;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import middleware.certifier.*;
import middleware.message.replication.GlobalEventMessage;
import middleware.reader.LogReader;
import middleware.message.ContentMessage;
import middleware.message.Message;
import middleware.message.WriteMessage;
import middleware.message.replication.CertifyWriteMessage;
import middleware.message.replication.FullState;
import middleware.reader.Pair;
import spread.SpreadException;

import java.io.IOException;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ServerImpl<STATE extends Serializable> implements Server {

    private final ClusterReplicationService replicationService;
    private final Serializer s;
    private final ManagedMessagingService mms;
    private final CompletableFuture<Void> runningCompletable;
    private final Connection databaseConnection;
    private ReentrantLock rl;

    //TODO remove/update
    private final Map<String, Address> writesRequests;
    public Certifier certifier;
    private final String privateName;
    private final LogReader logReader;
    private boolean isPaused;

    private final ExecutorService certifierExecutor;
    private final ExecutorService taskExecutor;


    public ServerImpl(int spreadPort, String privateName, int atomixPort, Connection databaseConnection, int totalServerCount, String logPath, List<GlobalEvent> events){
        this.privateName = privateName;
        this.logReader = new LogReader(logPath);
        this.databaseConnection = databaseConnection;
        this.replicationService = new ClusterReplicationService(spreadPort, privateName, this, totalServerCount, databaseConnection, events);
        this.runningCompletable = new CompletableFuture<>();
        this.rl = new ReentrantLock();
        this.certifier = new Certifier();
        this.writesRequests = new HashMap<>();
        this.s = new SerializerBuilder()
                .withRegistrationRequired(false)
                .build();
        Address myAddress = Address.from("localhost", atomixPort);
        this.mms = new NettyMessagingService(
                "server",
                myAddress,
                new MessagingConfig());
        this.isPaused = false;

        this.certifierExecutor = Executors.newFixedThreadPool(1);
        this.taskExecutor = Executors.newFixedThreadPool(8);

    }

    /**
     * Called from atomix clientListener, used to get the correct response from the extended
     * It is handled locally and doesnt need replication
     * server
     * @param message The body Message received
     * @return the message body of the response
     */
    public abstract Message handleMessage(Message message);

    /**
     * Called from atomix clientListener, used to get the writeSet after application custom preprocessors
     * the request and then it is passed down the certification pipeline
     * server
     * @param message The body Message received
     * @param updates The class that stores all data that should be persisted
     * @return the message body of the response
     */
    public abstract boolean handleWriteMessage(WriteMessage<?> message, StateUpdates<String, Serializable> updates);

    /**
     * Called from handleCertifierAnswer when a certified write operation arrived at a replicated server.
     * Needs to persist the incoming changes
     * server
     * @param message contains the state to persist
     */
    public abstract void updateStateFromCommitedWrite(CertifyWriteMessage<?> message);

    /**
     * Called from handleCertifierAnswer when a write request was made and is considered valid.
     * Needs to make changes effective
     * server
     * @param changes Objects changed and have to persist
     */
    public abstract void commit(Set<TaggedObject<String, Serializable>> changes) throws Exception;

    /**
     * Called from handleCertifierAnswer when a write request was made and is considered invalid.
     * Needs to cancel the transaction
     * server
     */
    public abstract void rollback();


    public abstract void handleGlobalEvent(GlobalEvent e);

    /**
     * Get of the state of the current Server
     * @return the state of the current Server
     */
    @Deprecated
    public abstract STATE getState();

    /**
     * Set of the state of the Server, used for extended classes to update their state, called when secondary server
     * receives the updated version
     * @param state the updated state of the server
     */
    @Deprecated
    public abstract void setState(STATE state);

    public void pause() {
        isPaused = true;
    }

    public void unpause() {
        isPaused = false;
    }

    public Collection<Pair<String, Long>> getQueries(int from){
        try {
            return logReader.getQueries(from);
        } catch (Exception ex) {
            ex.printStackTrace();
            //alterar
            return new LinkedList<>();
        }
    }

    public void updateQueries(Collection<Pair<String, Long>> queries, Connection c){
        try {
            System.out.println("Updating queries (size: " + queries.size() + ")");
            for(Pair<String, Long> pair : queries) {
                String query = pair.getKey();
                logReader.putTimeStamp(pair.getValue());
                c.prepareCall(query).execute();
                System.out.println("query: " + query);
            }
        } catch (SQLException | IOException ex) {
            ex.printStackTrace();
            try {
                this.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //TODO: Parar execução
        }
    }

    /**
     * Starts the server communication listeners. First it starts the ClusterReplicationService so that a replica that is
     * joining the server reaches a consistent state. When on a consistent state the server can listen to clients requests
     * server
     */
    //TODO pode ouvir os pedidos dos clientes mesmo sem estar consistente e guarda-os,
    // mas é discutível, porque um cliente pode ligar-se a outro e talvez ser atendido mais rapidamente
    @Override
    public void start() throws Exception {
        replicationService.start().thenAccept(x -> {
            startClientListener();
            System.out.println("Server : " + privateName + " Primary Server initialized");
        });
        runningCompletable.get();
    }

    /**
     * Called from regularMessageListener in ClusterReplicationServer when a write request passed through the certification
     * process and got answered.
     * server
     */

    //TODO não mexer sem perguntar !!
    protected void handleCertifierAnswer(CertifyWriteMessage<?> message){
        CompletableFuture.runAsync(() -> {
            boolean isWritable = certifier.isWritable(message.getSets(), message.getStartTimestamp());
            System.out.println("Server : " + privateName + " isWritable: " + isWritable);

            Address cli;
            try{
                rl.lock();
                cli = this.writesRequests.get(message.getId());
            } finally {
                rl.unlock();
            }
            try {
                if (isWritable) {
                    System.out.println("Server " + privateName + " commiting to db");
                    logReader.putTimeStamp(certifier.getTimestamp());
                    commit((Set<TaggedObject<String, Serializable>>) message.getState());

                } else {
                    System.out.println("Server " + privateName + " rolling back write from db");
                    rollback();
                }
            } catch (Exception e) {
                // TODO: verificar se parar o programa é a melhor opção e ver isto do sendReply
                sendReply(new ContentMessage<>(false), cli);
                // If exception should stop program
                try {
                    this.stop();
                } catch (SpreadException spreadException) {
                    spreadException.printStackTrace();
                }
            }
            if(isWritable)
                certifier.commit(message.getSets());
            if (cli != null)
                CompletableFuture.runAsync(() -> certifier.shutDownLocalStartedTransaction(message.getTables(),
                        message.getStartTimestamp()), taskExecutor);
                sendReply(new ContentMessage<>(isWritable), cli);
        }, certifierExecutor);
    }

    protected CompletableFuture<Void> handleGlobalEvent(GlobalEventMessage e){
        return CompletableFuture.runAsync(() -> handleGlobalEvent(e.getBody()));
    }

    protected CompletableFuture<Long> getSafeToDeleteTimestamp(){
        return CompletableFuture.supplyAsync(() -> certifier.getSafeToDeleteTimestamp(), certifierExecutor);
    }

    protected CompletableFuture<Void> evictStoredWriteSets(long ts){
        return CompletableFuture.runAsync(() -> certifier.evictStoredWriteSets(ts), certifierExecutor);
    }

    protected CompletableFuture<FullState> getState(int lowerBound, long latestTimestamp){
        return CompletableFuture.supplyAsync(() -> {
            HashMap<String, HashMap<Long, OperationalSets>> writes = certifier.getWriteSetsByTimestamp(latestTimestamp);
            try {
                List<Pair<String, Long>> queriesWithTS = logReader.getQueries(lowerBound);
                ArrayList<String> onlyQueries = new ArrayList<>();
                queriesWithTS.forEach(pair -> {onlyQueries.add(pair.getKey());});
                FullState state = new FullState(onlyQueries, writes);
                logReader.resetQueries();
                return state;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }, certifierExecutor);
    }

    public void rebuildCertifier(HashMap<String, HashMap<Long, OperationalSets>> c){
        this.certifier.addState(c);
    }

    private void sendReply(Message message, Address address) {
        mms.sendAsync(address, "reply", s.encode(message)).whenComplete((m, t) -> {
            if (t != null) {
                t.printStackTrace();
            }
        });
    }


    /**
     * Called from clientListener handler when a transactionMessage arrived. Starts a transaction by getting the current starting
     * timestamp and pre-processing the request.
     * server
     * @param cwm message
     * @return the information necessary to certify and replicate the transaction that is for now in a transient state
     */
    private void startTransaction(Address requester, CertifyWriteMessage<?> cwm) throws Exception {
        long ts = certifier.getTimestamp();
        certifier.transactionStarted(cwm.getTables(), ts);
        cwm.setTimestamp(ts);
        try {
            rl.lock();
            this.writesRequests.put(cwm.getId(), requester);
        } finally {
            rl.unlock();
        }
        replicationService.floodMessage(cwm);
        System.out.println("Server: " + privateName + " trying to commit " + cwm.getId());
    }

    /**
     * Provides middleware logic to receive and reply to clients requests.
     * server
     */
    public void startClientListener(){
        this.mms.start();
        mms.registerHandler("request", (requester,b) -> {
            if(isPaused)
                return;

            Message request = s.decode(b);
            try {
                if(request instanceof WriteMessage) {
                    System.out.println("Server " + privateName + " handling the request with group members, certification needed");
                    StateUpdates<String, Serializable> updates = new StateUpdatesBitSet<>();
                    if(handleWriteMessage((WriteMessage<?>) request, updates)) {
                        CertifyWriteMessage<?> cwm = new CertifyWriteMessage<>(updates.getSets(), (LinkedHashSet<TaggedObject<String, Serializable>>) updates.getAllUpdates());
                        startTransaction(requester, cwm);
                    } else {
                        // error
                        ContentMessage<Boolean> reply = new ContentMessage<>(false);
                        sendReply(reply, requester);
                    }
                } else {
                    System.out.println("Server " + privateName + " handling the request locally");
                    Message reply = handleMessage(request);
                    sendReply(reply, requester);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }, taskExecutor);
    }


    @Override
    public void stop() throws SpreadException {
        this.runningCompletable.complete(null);
        this.replicationService.stop();
        this.mms.stop();
    }

    public String getPrivateName() {
        return privateName;
    }

    public LogReader getLogReader() {
        return logReader;
    }

    public Certifier getCertifier() {
        return certifier;
    }

    public long getTimestamp() throws Exception {
        ResultSet resultSet = databaseConnection.prepareCall("SELECT \"timestamp\" FROM \"__certifier\" ORDER BY \"timestamp\" DESC LIMIT 1").executeQuery();
        if(resultSet.next()) {
            return resultSet.getLong("timestamp");
        } else {
            return -1;
        }
    }
}
