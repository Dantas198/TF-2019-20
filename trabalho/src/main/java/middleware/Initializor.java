package middleware;

import middleware.message.Message;
import middleware.message.replication.StateTransferMessage;
import spread.SpreadException;
import spread.SpreadMessage;

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

    public Initializor(ServerImpl server){
        this.server = server;
        this.messageQueue = new LinkedList<>();
        this.initializing = true;
    }

    public boolean isInitializing(SpreadMessage spreadMessage, Consumer<SpreadMessage> respondMessage){
        try {
            if(initializing){
                Message received = (Message) spreadMessage.getObject();
                if(received instanceof StateTransferMessage){
                    //TODO URGENTE
                    //server.setState(((StateTransferMessage) received).getState());
                    initializing = false;
                    for(SpreadMessage sm : messageQueue){
                        respondMessage.accept(sm);
                    }
                    messageQueue = null;
                } else {
                    messageQueue.add(spreadMessage);
                }
            }
        } catch (SpreadException e) {
            e.printStackTrace();
        }
         return initializing;
    }

    public void initialized(){
        this.initializing = false;
    }
}
