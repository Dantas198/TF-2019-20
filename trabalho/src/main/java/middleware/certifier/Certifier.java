package middleware.certifier;

import java.io.Serializable;
import java.util.*;

/**
 * Class that deals with certification logic.
 */

//TODO garbage collection
public class Certifier<V, K extends WriteSet<V>> implements Serializable {

    private long lowWaterMark;
    //Holds current global timestamp
    private long timestamp;
    //Stores the changes commited on a certain timestamp
    private final HashMap<String, LinkedHashMap<Long, Round<K>>> writesPerTable;


    //TODO arranjar controlo de concorrência
    //TODO líder encarrega-se de mandar a eviction message

    public Certifier(){
        this.lowWaterMark = -1;
        this.timestamp = 0;
        this.writesPerTable = new LinkedHashMap<>();
    }

    public Certifier(Certifier<V, K> c){
        this.lowWaterMark = c.getLowWaterMark();
        this.timestamp = c.getTimestamp();
        this.writesPerTable = new LinkedHashMap<>(c.getWritesPerTable());
    }

    public void addTables(List<String> tables){
        for(String table : tables)
            this.writesPerTable.put(table, new LinkedHashMap<>());
    }

    public boolean hasConflict(Map<String, K> ws, long ts) {

        if (ts < lowWaterMark) {
            System.out.println("Certifier: old timestamp arrived");
            return false;
        }

        for(Map.Entry<String, K> entry : ws.entrySet()){
            String table = entry.getKey();
            LinkedHashMap<Long, Round<K>> writes = writesPerTable.getOrDefault(table, new LinkedHashMap<>());

            for (long i = ts; i < timestamp; i++) {
                Round<K> set = writes.get(i);
                System.out.println(i);
                if(set.getWriteSet().intersects(entry.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    public synchronized void commitLocalStartedTransaction(Map<String, K> ws, long ts, String id) {

        for(Map.Entry<String, K> entry : ws.entrySet()){
            String table = entry.getKey();
            LinkedHashMap<Long, Round<K>> writes = this.writesPerTable.getOrDefault(table, new LinkedHashMap<>());

            writes.get(this.timestamp).addCommit(entry.getValue());
            writes.get(ts).removeStarted(id);
        }
        this.timestamp++;
    }


    public synchronized void commit(Map<String, K> ws) {
        //Commit also increases current timestamp
        for(Map.Entry<String, K> entry : ws.entrySet()){
            String table = entry.getKey();
            LinkedHashMap<Long, Round<K>> writes = this.writesPerTable.getOrDefault(table, new LinkedHashMap<>());

            Round<K> r = writes.get(this.timestamp);
            K bws = entry.getValue();
            if(r == null){
                r = new Round<>();
                r.addCommit(bws);
                writes.put(this.timestamp, r);

            }else
                r.addCommit(bws);
        }
        this.timestamp++;
    }

    public synchronized long getTimestamp(){
        return this.timestamp;
    }

    public synchronized void transactionStarted(Set<String> tables, long ts, String id) {
        for (String table : tables) {
            LinkedHashMap<Long, Round<K>> writes = this.writesPerTable.getOrDefault(table, new LinkedHashMap<>());

            Round<K> r = writes.get(ts);
            if (r == null) {
                r = new Round<K>();
                r.addStarted(id);
                writes.put(ts, r);
            } else
                r.addStarted(id);
        }
    }

    //TODO não suporta clientes lentos. Para suportar -> Force GC -> Round com Set de transactionId
    public synchronized long getSafeToDeleteTimestamp(){
        long maxTimestampToDelete = -1;
        for(LinkedHashMap<Long, Round<K>> writes : this.writesPerTable.values()){
            long minimumTimestamp = Long.MAX_VALUE;
            for(Map.Entry<Long, Round<K>> entry : writes.entrySet()){
                int numTR = entry.getValue().getNumTransactionRunning();
                // mal encontre um que não esteja a 0 não considera os restantes que possam estar a 0
                if(numTR != 0)
                    break;
                minimumTimestamp = numTR;
            }
            if(minimumTimestamp > maxTimestampToDelete)
                maxTimestampToDelete = minimumTimestamp;
        }
        return maxTimestampToDelete;
    }

    public void evictStoredWriteSets(long newLowWaterMark){
        for(LinkedHashMap<Long, Round<K>> writes : this.writesPerTable.values()){
            for(long i = this.lowWaterMark; i <= newLowWaterMark; i++){
                writes.remove(i);
            }
        }
        this.lowWaterMark = newLowWaterMark;
    }

    public HashMap<String, LinkedHashMap<Long, Round<K>>> getWritesPerTable(){
        return writesPerTable;
    }

    public long getLowWaterMark() {
        return lowWaterMark;
    }
}
