package middleware.certifier;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class that deals with certification logic.
 */


//TODO atrasar decremento na rounda para não atrasar escritas


public class Certifier<V, K extends WriteSet<V>> {

    //Last garbage collected timestamp limit
    private long lowWaterMark;

    //Holds current global timestamp
    private long timestamp;

    //Stores running transaction for each table and each timestamp
    private final HashMap<String, ConcurrentHashMap<Long, Integer>> runningTransactionsPerTable;

    //Stores the changes commited on a certain timestamp
    //TODO concurrent tbm?
    private final HashMap<String, HashMap<Long, WriteSet<V>>> writesPerTable;

    //TODO add tables no construtor ou dinâmicas?
    public Certifier(){
        this.lowWaterMark = -1;
        this.timestamp = 0;
        this.runningTransactionsPerTable = new HashMap<>();
        this.writesPerTable = new HashMap<>();
    }


    public Certifier(Certifier<V, K> c){
        this.lowWaterMark = c.getLowWaterMark();
        this.timestamp = c.getTimestamp();
        this.runningTransactionsPerTable = new HashMap<>();
        this.writesPerTable = new HashMap<>(c.getWritesPerTable());
    }

    public void addTables(List<String> tables){
        tables.forEach(t -> this.runningTransactionsPerTable.put(t, new ConcurrentHashMap<>()));
        tables.forEach(t -> this.writesPerTable.put(t, new HashMap<>()));
    }

    public boolean hasConflict(Map<String, K> ws, long ts) {
        if (ts < lowWaterMark) {
            System.out.println("Certifier: old timestamp arrived");
            return false;
        }

        for(Map.Entry<String, K> entry : ws.entrySet()) {
            String table = entry.getKey();
            HashMap<Long, WriteSet<V>> writes = writesPerTable.get(table);
            for (long i = ts; i < timestamp; i++) {
                WriteSet<V> set = writes.get(i);
                System.out.println(i);
                if(set.intersects(entry.getValue()))
                    return true;
            }
        }
        return false;
    }

    public void transactionStarted(Set<String> tables, long ts, String id) {
        for (String table : tables) {
            this.runningTransactionsPerTable.get(table)
                .compute(ts, (k,v) -> {
                    if (v == null)
                        v = 0;
                    return ++v;
                });
        }
    }

    public void shutDownLocalStartedTransaction(Set<String> tables, long ts) {
        tables.forEach(table -> this.runningTransactionsPerTable.get(table).computeIfPresent(ts, (k, v) -> ++v));
    }

    //Commit also increases current timestamp
    public void commit(Map<String, K> ws) {
        ws.forEach((table, writeSet) -> this.writesPerTable.get(table).put(this.timestamp, writeSet));
        this.timestamp++;
    }

    public long getSafeToDeleteTimestamp(){
        long maxTimestampToDelete = -1;
        for(ConcurrentHashMap<Long, Integer> running : this.runningTransactionsPerTable.values()){
            long minimumTimestamp = Long.MAX_VALUE;

            for(Map.Entry<Long, Integer> entry : running.entrySet()){
                int numTR = entry.getValue();
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
        for(String table : writesPerTable.keySet()){
            for(long i = this.lowWaterMark; i <= newLowWaterMark; i++){
                this.writesPerTable.get(table).remove(i);
                this.runningTransactionsPerTable.get(table).remove(i);
            }
        }
        this.lowWaterMark = newLowWaterMark;
    }

    public long getTimestamp(){
        return this.timestamp;
    }

    public long getLowWaterMark() {
        return this.lowWaterMark;
    }

    public HashMap<String, HashMap<Long, WriteSet<V>>> getWritesPerTable(){
        return this.writesPerTable;
    }

}
