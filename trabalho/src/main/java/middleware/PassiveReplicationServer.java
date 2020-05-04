package middleware;

import middleware.listeners.SecondaryServerListener;
import middleware.message.ContentMessage;
import middleware.message.Message;
import server.GandaGotaServer;
import spread.AdvancedMessageListener;
import spread.SpreadConnection;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

public abstract class PassiveReplicationServer<STATE extends Serializable> implements Server {

    private String privateName;
    private AdvancedMessageListener messageListener;
    private SpreadConnection spreadConnection;
    private SpreadGroup spreadGroup;
    private int port;
    private CompletableFuture<Void> runningCompletable;

    public PassiveReplicationServer(int port, String privateName){
        this.privateName = privateName;
        this.port = port;
        this.spreadGroup = new SpreadGroup();
        this.spreadConnection = new SpreadConnection();
        this.runningCompletable = new CompletableFuture<>();
        this.messageListener = new SecondaryServerListener(spreadConnection, privateName, this);
    }

    @Override
    public void start() throws Exception {
        this.spreadConnection.connect(InetAddress.getByName("localhost"), port, this.privateName,
                false, true);
        this.spreadGroup.join(this.spreadConnection, "grupo");
        this.spreadConnection.add(this.messageListener);
        runningCompletable.get();
    }

    @Override
    public void stop() throws Exception {
        this.runningCompletable.complete(null);
    }

    /**
     * Called when necessary from regularMessageReceived (Spread), used to get the correct response from the extended
     * server
     * @param message The body Message received
     * @return the message body of the response
     */
    public abstract Message handleMessage(Message message);

    public void setMessageListener(AdvancedMessageListener messageListener){
        this.spreadConnection.remove(this.messageListener);
        this.messageListener = messageListener;
        this.spreadConnection.add(this.messageListener);
    }

    /**
     * Used to respond to all Servers in the current spread group
     * @param message the body message that should be passed to all Servers
     * @throws Exception
     */
    public void floodMessage(Message message) throws Exception{
        floodMessage(message, this.spreadGroup);
    }

    /**
     * Used to respond to all Servers connected in the corresponding spread group
     * @param message
     * @param sg
     * @throws Exception
     */
    public void floodMessage(Message message, SpreadGroup sg) throws Exception{
        SpreadMessage m = new SpreadMessage();
        m.addGroup(sg);
        m.setObject(message);
        m.setReliable();
        spreadConnection.multicast(m);
        System.out.println("Flooding to group ("+ this.spreadGroup+ "): " + message);
    }

    /**
     * Get of the state of the current Server
     * @return the state of the current Server
     */
    public abstract STATE getState();

    /**
     * Set of the state of the Server, used for extended classes to update their state, called when secondary server
     * receives the updated version
     * @param state the updated state of the server
     */
    public abstract void setState(STATE state);

    /**
     * Method used to respond to the Sender the message defined in the handleMessage abstract method
     * @param spreadMessage
     */
    public void respondMessage(SpreadMessage spreadMessage) {
        try {
            Message received = (Message) spreadMessage.getObject();
            System.out.println("Received message with id: "  + received.getId());
            Message bodyResponse = handleMessage(received).from(received);
            System.out.println("Handled message with id: "  + received.getId());
            Message response = new ContentMessage<>(bodyResponse);
            floodMessage(response, spreadMessage.getSender());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new GandaGotaServer(4803, "2");
        server.start();
    }
}
