package middleware.Certifier;

import java.util.LinkedHashMap;


/**
 * Class that deals with certification logic.
 */

//TODO garbage collection
// Não está grande coisa isto dos timestamps
public class Certifier {
    //Holds current global timestamp
    private long timestamp;
    //Stores the changes commited on a certain timestamp
    private LinkedHashMap<Long, BitWriteSet> writes;


    //TODO arranjar controlo de concorrência

    public Certifier(){
        this.timestamp = 0;
        this.writes = new LinkedHashMap<>();
    }

    public Certifier(long timestamp, LinkedHashMap<Long, BitWriteSet> writes){
        this.timestamp = timestamp;
        this.writes = writes;
    }

    public boolean hasConflict(BitWriteSet ws, long ts) {
        for (long i = ts; i <= timestamp; i++) {
            BitWriteSet set = writes.get(i);
                if(set.intersects(ws)) {
                    return false;
                }
            }
       return true;
    }

    public void commit(BitWriteSet ws){
        //Commit also increases current timestamp
        nextTimestamp();
        this.writes.put(this.timestamp, ws);
    }

    public synchronized void nextTimestamp(){
        this.timestamp++;
    }

    public synchronized long getTimestamp(){
        return this.timestamp;
    }
}
