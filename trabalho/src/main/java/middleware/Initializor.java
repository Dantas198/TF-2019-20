package middleware;

import middleware.message.Message;
import middleware.message.StateMessage;
import spread.SpreadException;
import spread.SpreadMessage;

import java.util.LinkedList;
import java.util.Queue;

public class Initializor {

    private Queue<SpreadMessage> messageQueue;
    private Boolean initializing;
    private ServerImpl server;

    public Initializor(ServerImpl server){
        this.server = server;
        this.messageQueue = new LinkedList<>();
        this.initializing = true;
    }

    public boolean isInitializing(SpreadMessage spreadMessage){
        try {
            Message received = (Message) spreadMessage.getObject();
            if(received instanceof StateMessage){
                StateMessage stateMessage = (StateMessage) received;
                if(stateMessage.getServerName().equals(server.getPrivateName())){
                    initializing = false;
                    messageQueue = null;
                }
            }

            if(initializing){
                if(received instanceof StateMessage){
                    initializing = false;
                    for(SpreadMessage sm : messageQueue){
                        server.respondMessage(sm);
                    }
                    messageQueue = null;
                    server.setState(((StateMessage) received).getBody());
                } else {
                    messageQueue.add(spreadMessage);
                }
            }
        } catch (SpreadException e) {
            e.printStackTrace();
        }
        return initializing;
    }
}
