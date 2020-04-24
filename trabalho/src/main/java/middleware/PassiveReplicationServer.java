package middleware;

import spread.AdvancedMessageListener;
import spread.SpreadConnection;
import spread.SpreadGroup;
import spread.SpreadMessage;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.concurrent.CompletableFuture;

public abstract class PassiveReplicationServer implements Server {

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
    }

    @Override
    public void start() throws Exception {
        this.spreadConnection.connect(InetAddress.getByName("localhost"), port, this.privateName,
                false, true);
        this.spreadGroup.join(this.spreadConnection, "grupo");
        runningCompletable.get();
    }

    @Override
    public void stop() throws Exception {
        this.runningCompletable.complete(null);
    }

    public void floodMessage(Serializable message) throws Exception{
        SpreadMessage m = new SpreadMessage();
        m.addGroup(this.spreadGroup);
        m.setObject(message);
        m.setReliable();
        spreadConnection.multicast(m);
        System.out.println("Flooding to group ("+ this.spreadGroup+ "): " + message);
    }

    public abstract void handleMessage(Object message);
}
