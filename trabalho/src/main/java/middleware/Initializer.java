package middleware;

import middleware.message.Message;
import middleware.message.replication.*;
import spread.SpreadMessage;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;

//TODO Servidores à escuta veem que entrou um servidor ou novo ou que tinha ido abaixo.
// Enviam o número de linhas que teem (Refletir sobre a melhor maneiro)
// Initializor espera por isso e compara com os seus logs
// Envia pedido do que falta e recebe o estado que falta

public class Initializer {

    private Queue<SpreadMessage> messageQueue;
    private Boolean initializing;
    private ServerImpl server;
    private ClusterReplicationService service;
    private Connection connection;

    public Initializer(ServerImpl server, ClusterReplicationService service, Connection connection){
        this.server = server;
        this.messageQueue = new LinkedList<>();
        this.initializing = true;
        this.service = service;
        this.connection = connection;
    }

    public boolean isInitializing(SpreadMessage spreadMessage, Consumer<SpreadMessage> respondMessage){
        try {
            if(initializing){
                Message received = (Message) spreadMessage.getObject();
                // apagar este if e o seu conteudo quando se remover o state
                if(received instanceof StateTransferMessage){
                    //TODO URGENTE
                    //server.setState(((StateTransferMessage) received).getState());
                    System.out.println("Received state transfer");
                    StateTransferMessage stm = (StateTransferMessage) received;
                    ArrayList<String> logs = stm.getState().getBusinessState();
                    server.rebuildCertifier(stm.getState().getCertifierState());
                    server.updateQueries(logs, server.getLogReader().getPath(), this.connection);
                    initializing = false;
                    for(SpreadMessage sm : messageQueue){
                        respondMessage.accept(sm);
                    }
                    messageQueue = null;
                } else if (received instanceof GetLengthRequestMessage){
                    System.out.println("Received logs length request");
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
}
