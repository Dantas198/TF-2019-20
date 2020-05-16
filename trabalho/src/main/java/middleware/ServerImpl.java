package middleware;

import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import middleware.logreader.LogReader;
import middleware.message.Message;
import middleware.message.WriteMessage;
import middleware.message.replication.CertifyWriteMessage;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ServerImpl<STATE extends Serializable> implements Server {
    private final ClusterReplicationService spreadService;
    private final ExecutorService e;
    private final Serializer s;
    private final ManagedMessagingService mms;
    private final CompletableFuture<Void> runningCompletable;
    private ReentrantLock rl;
    private Map<String, CompletableFuture<Boolean>> pendingWrites;
    private String privateName;
    private LogReader logReader;

    public ServerImpl(int spreadPort, String privateName, int atomixPort){
        this.privateName = privateName;
        this.spreadService = new ClusterReplicationService(spreadPort, privateName, this);
        this.e = Executors.newFixedThreadPool(1);
        this.runningCompletable = new CompletableFuture<>();
        this.rl = new ReentrantLock();
        this.pendingWrites = new HashMap<>();
        this.s = new SerializerBuilder().withRegistrationRequired(false).build();
        Address myAddress = Address.from("localhost", atomixPort);
        this.mms = new NettyMessagingService(
                "server",
                myAddress,
                new MessagingConfig());
        this.logReader = new LogReader("./testdb.sql.log");
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
    public abstract CertifyWriteMessage<?> preprocessMessage(Message message);

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
     */
    public abstract void commit();

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

    public Collection<String> getQueries(int from){
        try {
            return logReader.getQueries(from);
        } catch (Exception ex) {
            ex.printStackTrace();
            //alterar
            return new LinkedList<>();
        }
    }

    public void updateQueries(Collection<String> queries){
        try {
            Connection c = DriverManager.getConnection("jdbc:hsqldb:file:testdb;shutdown=true;hsqldb.sqllog=2", "", "");
            for(String querie : queries) {
                c.prepareStatement(querie).execute();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
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
        spreadService.start().thenAccept(x -> {
            startClientListener();
            System.out.println("Primary Server initialized");
        });
        runningCompletable.get();
    }

    /**
     * Called from regularMessageListener in ClusterReplicationServer when a write request passed through the certification
     * process and got answered.
     * server
     */
    protected void handleCertifierAnswer(CertifyWriteMessage<?> message, boolean isWritable){
        try{
            rl.lock();
            CompletableFuture<Boolean> res = this.pendingWrites.get(message.getId());
            rl.unlock();
            // se for null então não foi iniciada a escrita neste servidor
            if(res == null){
                if(isWritable) {
                    updateStateFromCommitedWrite(message);
                    //assumimos que a função anterior não falha..senão está tudo perdido...talvez resolver com os acks
                    //Só é incrementado o timestamp e dado o "commit" quando as mudanças estão feitas na BD
                    spreadService.certifier.commit(message.getWriteSet());
                }
            }
            else{
                //Eu acho que a execução do res após o complete continua na thread do spread ....terá de ser assim para funcionar
                //Chama a callback de onde foi iniciado o pedido de escrita
                res.complete(isWritable);
            }
        }finally {
            rl.unlock();
        }
    }

    /**
     * Called from clientListener handler when a writeMessage arrived. Starts a transaction by getting the current starting
     * timestamp and pre-processing the request.
     * server
     * @param reqm request being made
     * @return the information necessary to certify and replicate the transaction that is for now in a transient state
     */
    private CertifyWriteMessage<?> startTransaction(Message reqm) {
        long ts = spreadService.certifier.getTimestamp();
        //pre-processamento: colocar transação com estado não commit e calcular o estado resultado
        CertifyWriteMessage<?> cwm = preprocessMessage(reqm);
        cwm.setTimestamp(ts);
        return cwm;
    }

    /**
     * Called from clientListener handler when a writeMessage arrived. Tries to commit a transient transaction and also
     * formulates logic of the incoming answer
     * server
     * @param cwm message necessary for the certification and replication of the transaction
     * @param a address of the requester
     */
    private void tryCommit(Address a, CertifyWriteMessage<?> cwm) throws Exception {
        try {
            CompletableFuture<Boolean> res = new CompletableFuture<>();
            rl.lock();
            this.pendingWrites.put(cwm.getId(), res);
            rl.unlock();
            res.thenAccept(isWritable -> {
                if (isWritable) {
                    //Primeiro persite o estado
                    commit();
                    //Atualiza as transações que foram feitas
                    spreadService.certifier.commit(cwm.getWriteSet());
                } else {
                    rollback();
                }
                // TODO msg de resposta
                Message message = new Message();
                System.out.println("Sending response message: " + message);
                mms.sendAsync(a, "reply", s.encode(message)).whenComplete((m, t) -> {
                    if (t != null) {
                        t.printStackTrace();
                    }
                });
            });
            spreadService.floodCertifyMessage(cwm);
        }finally {
            rl.unlock();
        }
    }


    /**
     * Provides middleware logic to receive and reply to clients requests.
     * server
     */
    public void startClientListener(){
        this.mms.start();
        mms.registerHandler("request", (a,b) -> {
            Message reqm = s.decode(b);
            try {
                if(reqm instanceof WriteMessage) {
                    System.out.println("Handling the request with group members");
                    CertifyWriteMessage<?> cwm = startTransaction(reqm);
                    tryCommit(a, cwm);
                }
                else{
                    System.out.println("Handling the request locally");
                    handleMessage(reqm);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        },e);
    }

    @Override
    public void stop() throws Exception {
        this.runningCompletable.complete(null);
    }

    public String getPrivateName() {
        return privateName;
    }
}
