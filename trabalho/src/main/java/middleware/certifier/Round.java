package middleware.certifier;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Round<K> implements Serializable {
    private K writeSet;
    //transactions started on this round
    private final Set<String> runningTransactions;

    public Round(){
        writeSet = null;
        this.runningTransactions = new HashSet<>();
    }

    public void addCommit(K writeSet){
        this.writeSet = writeSet;
    }

    public void addStarted(String id){
        this.runningTransactions.add(id);
    }

    public void removeStarted(String id){
        this.runningTransactions.remove(id);
    }

    public K getWriteSet() {
        return writeSet;
    }

    public int getNumTransactionRunning() {
        return this.runningTransactions.size();
    }
}
