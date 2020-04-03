package servers;

import java.util.function.Consumer;

public class ReplicationState {
    int requestId;
    int numAcks;
    boolean primaryConfirmation;
    Consumer<Void> callback;

    public ReplicationState(int id, Consumer<Void> callback){
        this.requestId = id;
        this.numAcks=0;
        this.primaryConfirmation = false;
        this.callback = callback;
    }

    public void incrAcks(){
        this.numAcks++;
    }

    public void primaryConfirmed(){
        this.primaryConfirmation = true;
    }


}
