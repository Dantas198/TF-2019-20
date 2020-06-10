package middleware;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import middleware.certifier.Certifier;
import middleware.certifier.NoTableDefinedException;
import middleware.certifier.TaggedObject;
import middleware.certifier.WriteSet;
import middleware.logreader.LogReader;
import middleware.message.ContentMessage;
import middleware.message.Message;
import middleware.message.WriteMessage;
import middleware.message.replication.CertifyWriteMessage;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ServerImpl<K, W extends WriteSet<K>, STATE extends Serializable> implements Server {

    private final ClusterReplicationService<K,W> replicationService;
    private final Serializer s;
    private final ManagedMessagingService mms;
    private final CompletableFuture<Void> runningCompletable;
    private ReentrantLock rl;

    //TODO remove/update
    private final Map<String, Address> writesRequests;
    public Certifier<K, W> certifier;
    private final String privateName;
    private final LogReader logReader;
    private boolean isPaused;

    private final ExecutorService certifierExecutor;
    private final ExecutorService taskExecutor;

    public ServerImpl(int spreadPort, String privateName, int atomixPort, Connection databaseConnection){
        this.privateName = privateName;
        // TODO numero de servidores max/total
        this.logReader = new LogReader("db/" + privateName + ".log"); //TODO: passar como argumento
        this.replicationService = new ClusterReplicationService(spreadPort, privateName, this, 3, logReader, databaseConnection);
        this.runningCompletable = new CompletableFuture<>();
        this.rl = new ReentrantLock();
        this.certifier = new Certifier<>();
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
     * @return the message body of the response
     */
    //TODO: Converter para WriteMessage visto que só aceita write message
    public abstract CertifyWriteMessage<W,?> handleWriteMessage(WriteMessage<?> message);

    /**
     * Called from handleCertifierAnswer when a certified write operation arrived at a replicated server.
     * Needs to persist the incoming changes
     * server
     * @param message contains the state to persist
     */
    public abstract void updateStateFromCommitedWrite(CertifyWriteMessage<W,?> message);

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

    /**
     * Set of tables for certifier module
     */
    public void addTablesToCertifier(List<String> tables){
        this.certifier.addTables(tables);
    }

    public Collection<String> getQueries(int from){
        try {
            return logReader.getQueries(from);
        } catch (Exception ex) {
            ex.printStackTrace();
            //alterar
            return new LinkedList<>();
        }
    }

    public void updateQueries(Collection<String> queries, Connection c){
        try {
            System.out.println("Updating queries (size: " + queries.size() + ")");
            for(String query : queries) {
                c.prepareCall(query).execute();
                System.out.println("query: " + query);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            System.exit(1);
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
    protected void handleCertifierAnswer(CertifyWriteMessage<W,?> message){
        CompletableFuture.runAsync(() -> {
            boolean isWritable = !certifier.hasConflict(message.getWriteSets(), message.getStartTimestamp());
            System.out.println("Server : " + privateName + " isWritable: " + isWritable);

            Address cli;
            try{
                rl.lock();
                cli = this.writesRequests.get(message.getId());
            }finally {
                rl.unlock();
            }
            try {
                CompletableFuture.runAsync(() -> certifier.shutDownLocalStartedTransaction(message.getTables(),
                    message.getStartTimestamp()), taskExecutor);
                if (isWritable) {
                    System.out.println("Server " + privateName + " commiting to db");
                    commit((Set<TaggedObject<String, Serializable>>) message.getState());
                } else {
                    System.out.println("Server " + privateName + " rolling back write from db");
                    rollback();
                }
            } catch (Exception e) {
                // TODO: verificar se parar o programa é a melhor opção
                // If exception should stop program
                System.exit(1);
            }
            if(isWritable)
                certifier.commit(message.getWriteSets());
            sendReply(new ContentMessage<>(isWritable), cli);
        }, certifierExecutor);
    }

    protected void handleCertifierAnswerCommitAsync(CertifyWriteMessage<W,?> message){
        CompletableFuture.runAsync(() -> {
            boolean isWritable = !certifier.hasConflict(message.getWriteSets(), message.getStartTimestamp());
            System.out.println("Server : " + privateName + " isWritable: " + isWritable);

            Address cli;
            try{
                rl.lock();
                cli = this.writesRequests.get(message.getId());
            }finally {
                rl.unlock();
            }
            CompletableFuture.runAsync(() -> {
                try {
                    CompletableFuture.runAsync(() -> certifier.shutDownLocalStartedTransaction(message.getTables(),
                            message.getStartTimestamp()), taskExecutor);
                    if (isWritable) {
                        System.out.println("Server " + privateName + " commiting to db");
                        commit((Set<TaggedObject<String, Serializable>>) message.getState());
                    } else {
                        System.out.println("Server " + privateName + " rolling back write from db");
                        rollback();
                    }
                } catch (Exception e) {
                    // TODO: verificar se parar o programa é a melhor opção
                    // If exception should stop program
                    System.exit(1);
                }
            }, taskExecutor).thenAccept((x) -> sendReply(new ContentMessage<>(isWritable), cli));
            if(isWritable)
                certifier.commit(message.getWriteSets());
            sendReply(new ContentMessage<>(isWritable), cli);
        }, certifierExecutor);
    }


    protected CompletableFuture<Long> getSafeToDeleteTimestamp(){
        return CompletableFuture.supplyAsync(() -> certifier.getSafeToDeleteTimestamp(), certifierExecutor);
    }

    protected CompletableFuture<Void> evictStoredWriteSets(long ts){
        return CompletableFuture.runAsync(() -> certifier.evictStoredWriteSets(ts), certifierExecutor);
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
    private void startTransaction(Address requester, CertifyWriteMessage<W,?> cwm) throws Exception {
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
                    startTransaction(requester, handleWriteMessage((WriteMessage<?>) request)); // TODO onde está o reply?
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

    public void rebuildCertifier(Certifier<K,W> c){
        this.certifier = new Certifier<>(c);
    }


    @Override
    public void stop() {
        this.runningCompletable.complete(null);
    }

    public String getPrivateName() {
        return privateName;
    }
}
