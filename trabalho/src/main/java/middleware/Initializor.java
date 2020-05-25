package middleware;

import middleware.message.Message;
import middleware.message.replication.StateLengthRequestMessage;
import middleware.message.replication.StateTransferMessage;
import spread.SpreadException;
import spread.SpreadMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Consumer;
//TODO Servidores à escuta veem que entrou um servidor ou novo ou que tinha ido abaixo.
// Enviam o número de linhas que teem (Refletir sobre a melhor maneiro)
// Initializor espera por isso e compara com os seus logs
// Envia pedido do que falta e recebe o estado que falta
public class Initializor {

    private Queue<SpreadMessage> messageQueue;
    private Boolean initializing;
    private ServerImpl server;
    private ClusterReplicationService service;

    public Initializor(ServerImpl server, ClusterReplicationService service){
        this.server = server;
        this.messageQueue = new LinkedList<>();
        this.initializing = true;
        this.service = service;
    }

    public boolean isInitializing(SpreadMessage spreadMessage, Consumer<SpreadMessage> respondMessage){
        try {
            if(initializing){
                Message received = (Message) spreadMessage.getObject();
                // apagar este if e o seu conteudo quando se remover o state
                if(received instanceof StateTransferMessage){
                    //TODO URGENTE
                    //server.setState(((StateTransferMessage) received).getState());
                    ArrayList<String> logs = (ArrayList<String>) ((StateTransferMessage) received).getState();
                    server.updateQueries(logs);
                    initializing = false;
                    for(SpreadMessage sm : messageQueue){
                        respondMessage.accept(sm);
                    }
                    messageQueue = null;
                } else if (received instanceof StateLengthRequestMessage){
                    System.out.println("Received logs length request");
                    Message logsLength = new StateLengthRequestMessage(service.getLogReader().size());
                    service.noAgreementFloodMessage(logsLength, spreadMessage.getSender());
                } else {
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
