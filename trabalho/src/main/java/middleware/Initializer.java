package middleware;

import middleware.certifier.WriteSet;
import middleware.message.Message;
import middleware.message.replication.*;
import spread.SpreadMessage;

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * Used to manage the state update and transfer for joining servers on the ClusterReplicationService
 * @param <K>
 * @param <W>
 */
public class Initializer<K, W extends WriteSet<K>> {

    private Queue<SpreadMessage> messageQueue;
    private Boolean initializing;
    private ServerImpl<K,W,?> server;
    private ClusterReplicationService<K,W> service;
    private Connection connection;
    private String privateName;

    public Initializer(ServerImpl<K,W,?> server, ClusterReplicationService<K,W> service, Connection connection, String privateName){
        this.server = server;
        this.messageQueue = new LinkedList<>();
        this.initializing = true;
        this.service = service;
        this.connection = connection;
        this.privateName = privateName;
    }

    public boolean isInitializing(SpreadMessage spreadMessage, Consumer<SpreadMessage> respondMessage){
        try {
            if(initializing){
                Message received = (Message) spreadMessage.getObject();
                // apagar este if e o seu conteudo quando se remover o state
                if(received instanceof StateTransferMessage){
                    //TODO URGENTE
                    //server.setState(((StateTransferMessage) received).getState());
                    System.out.println(privateName + ": RegularMessage received -> State Transfer");
                    StateTransferMessage<K> stm = (StateTransferMessage<K>) received;
                    ArrayList<String> logs = stm.getState().getBusinessState();
                    server.rebuildCertifier(stm.getState().getCertifierState());
                    server.updateQueries(logs, server.getLogReader().getPath(), this.connection);
                    initializing = false;
                    for(SpreadMessage sm : messageQueue){
                        respondMessage.accept(sm);
                    }
                    messageQueue = null;
                } else if (received instanceof GetLengthRequestMessage){
                    System.out.println(privateName + ": RegularMessage received -> State Length Request");
                    int logSize = Math.max(0, server.getLogReader().size());
                    long latestTimestamp = server.certifier.getTimestamp();
                    Message logsLength = new StateLengthReplyMessage(new ReplicaLatestState(latestTimestamp, logSize));
                    service.noAgreementFloodMessage(logsLength, spreadMessage.getSender());
                }  else {
                    messageQueue.add(spreadMessage);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
         return initializing;
    }

    public void initialized(){
        this.initializing = false;
    }

    public void reset() {
        this.initializing = false;
    }
}
