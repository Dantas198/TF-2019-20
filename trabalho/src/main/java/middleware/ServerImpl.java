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
import middleware.reader.Pair;
import spread.SpreadException;

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ServerImpl implements Server {

    private final ClusterReplicationService replicationService;
    private final Serializer s;
    private final ManagedMessagingService mms;
    private final CompletableFuture<Void> runningCompletable;

    private Connection databaseConnection;
    private ReentrantLock rl;

    private final Map<String, Address> writesRequests;
    public Certifier certifier;
    private final String privateName;
    private final LogReader logReader;
    private boolean isPaused;
    private String connectionURL;

    private final ExecutorService certifierExecutor;
    private final ExecutorService taskExecutor;


    public ServerImpl(int spreadPort, String privateName, int atomixPort, String connectionURL, int totalServerCount,
                      String logPath, List<GlobalEvent> events) throws Exception{
        this.privateName = privateName;
        this.logReader = new LogReader(logPath);
        this.connectionURL = connectionURL;
        this.databaseConnection = DriverManager.getConnection(connectionURL);
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
        rebuildCertifier();

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
     * Called from handleCertifierAnswer when a write request was made and is considered valid.
     * Needs to make changes effective
     * server
     * @param changes Objects changed and have to persist
     * @param databaseConnection
     */
    public abstract void commit(Set<TaggedObject<String, Serializable>> changes, Connection databaseConnection) throws Exception;

    /**
     * Called from handleCertifierAnswer when a write request was made and is considered invalid.
     * Needs to cancel the transaction
     * server
     */
    public abstract void rollback(CertifyWriteMessage<?> message);


    public abstract void handleGlobalEvent(GlobalEventMessage e);

    public void pause() {
        isPaused = true;
    }

    public void unpause() {
        isPaused = false;
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
        }
    }

    /**
     * Starts the server communication listeners. First it starts the ClusterReplicationService so that a replica that is
     * joining the server reaches a consistent state. When on a consistent state the server can listen to clients requests
     * server
     */
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
                    long commitTs = certifier.getTimestamp();
                    logReader.putTimeStamp(commitTs);
                    save(commitTs, message.getSets());
                    databaseConnection.setAutoCommit(false);
                    commit((Set<TaggedObject<String, Serializable>>) message.getState(), databaseConnection);
                    certifier.commit(message.getSets());
                    databaseConnection.commit();
                } else {
                    System.out.println("Server " + privateName + " rolling back write from db");
                    rollback(message);
                }
            } catch (Exception e) {
                sendReply(new ContentMessage<>(false), cli);
                // If exception should stop program
                this.stop();
            }

            if (cli != null) {
                CompletableFuture.runAsync(() -> certifier.shutDownLocalStartedTransaction(message.getTables(),
                        message.getStartTimestamp()), taskExecutor);
                sendReply(new ContentMessage<>(isWritable), cli);
            }
        }, certifierExecutor);
    }

    protected CompletableFuture<Long> getSafeToDeleteTimestamp(){
        return CompletableFuture.supplyAsync(() -> certifier.getSafeToDeleteTimestamp(), certifierExecutor);
    }

    protected CompletableFuture<Void> evictStoredWriteSets(long ts){
        return CompletableFuture.runAsync(() -> certifier.evictStoredWriteSets(ts), certifierExecutor);
    }

    public void rebuildCertifier(HashMap<String, HashMap<Long, OperationalSets>> c){
        this.certifier.addState(c);
    }

    public void rebuildCertifier() throws SQLException, IOException, ClassNotFoundException {
        HashMap<String, HashMap<Long, OperationalSets>> map = new HashMap<>();
        ResultSet rs = databaseConnection.prepareCall("SELECT * FROM \"__certifier\"").executeQuery();
        while(rs.next()) {
            String table = rs.getString("table");
            long timestamp = rs.getLong("timestamp");
            byte [] data = Base64.getDecoder().decode(rs.getString("keys"));
            ObjectInputStream ois = new ObjectInputStream(
                    new ByteArrayInputStream(data));
            OperationalSets operationalSets = (OperationalSets) ois.readObject();
            map.computeIfAbsent(table, k -> new HashMap<>()).put(timestamp, operationalSets);
            this.certifier.setTimestamp(Long.max(this.certifier.getTimestamp(), timestamp));
            this.certifier.setLowWaterMark(Long.min(this.certifier.getLowWaterMark(), timestamp));
        }
        rebuildCertifier(map);
    }

    public void save(Long timestamp, Map<String, OperationalSets> operations) throws SQLException, IOException {
        PreparedStatement ps = databaseConnection.prepareStatement("INSERT INTO \"__certifier\" (\"timestamp\", \"table_name\", \"keys\") VALUES (?, ?, ?)");
        for (Map.Entry<String, OperationalSets> operation : operations.entrySet()) {
            String table = operation.getKey();
            OperationalSets operationalSets = operation.getValue();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream( baos );
            oos.writeObject( operationalSets );
            oos.close();
            String operationalSetsEncoded = Base64.getEncoder().encodeToString(baos.toByteArray());
            ps.setLong(1, timestamp);
            ps.setString(2, table);
            ps.setString(3, operationalSetsEncoded);
            ps.execute();
        }
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


    public void resetDBConnection() throws Exception{
        this.databaseConnection.close();
        this.databaseConnection = DriverManager.getConnection(connectionURL);
    }

    @Override
    public void stop() {
        this.runningCompletable.complete(null);
        try {
            this.replicationService.stop();
        } catch (SpreadException e) {
            System.exit(1);
        }
        this.mms.stop();
    }

    public Connection getDatabaseConnection() {
        return databaseConnection;
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

    public GlobalEventMessage createEvent(GlobalEvent e) {
        return new GlobalEventMessage(e);
    }
}
