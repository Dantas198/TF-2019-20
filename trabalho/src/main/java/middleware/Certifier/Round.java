package middleware.Certifier;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Round implements Serializable {
    private BitWriteSet writeSet;
    //transactions started on this round
    private Set<String> runningTransactions;

    public Round(){
        writeSet = null;
        this.runningTransactions = new HashSet<>();
    }

    public void addCommit(BitWriteSet writeSet){
        this.writeSet = writeSet;
    }

    public void addStarted(String id){
        this.runningTransactions.add(id);
    }

    public void removeStarted(String id){
        this.runningTransactions.remove(id);
    }

    public BitWriteSet getWriteSet() {
        return writeSet;
    }

    public int getNumTransactionRunning() {
        return this.runningTransactions.size();
    }
}
