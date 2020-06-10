package middleware.certifier;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class that deals with certification logic.
 */


//TODO atrasar decremento na rounda para não atrasar escritas

public class Certifier<V, K extends WriteSet<V>> implements Serializable {

    //Last garbage collected timestamp limit
    private long lowWaterMark;

    //Holds current global timestamp
    private long timestamp;

    private ReadWriteLock rwl;

    //Stores running transaction for each table and each timestamp
    private final HashMap<String, ConcurrentHashMap<Long, Integer>> runningTransactionsPerTable;

    //Stores the changes commited on a certain timestamp
    //TODO concurrent tbm?
    private final HashMap<String, HashMap<Long, WriteSet<V>>> writesPerTable;

    public Certifier(){
        this.lowWaterMark = -1;
        this.timestamp = 0;
        this.runningTransactionsPerTable = new HashMap<>();
        this.writesPerTable = new HashMap<>();
        this.rwl = new ReentrantReadWriteLock();
    }


    public Certifier(Certifier<V, K> c){
        this.lowWaterMark = c.getLowWaterMark();
        this.timestamp = c.getTimestamp();
        this.runningTransactionsPerTable = new HashMap<>();
        this.writesPerTable = new HashMap<>(c.getWritesPerTable());
    }

    public void addState(HashMap<String, HashMap<Long, WriteSet<V>>> c){
        c.forEach((k,v) -> v.forEach((ts, ws) -> {
            this.writesPerTable.putIfAbsent(k, new HashMap<>());
            this.writesPerTable.get(k).put(ts, ws);
            this.runningTransactionsPerTable.putIfAbsent(k, new ConcurrentHashMap<>());
            this.runningTransactionsPerTable.get(k).put(ts, 0);
        }));
    }

    public void addTables(List<String> tables){
        tables.forEach(t -> this.runningTransactionsPerTable.put(t, new ConcurrentHashMap<>()));
        tables.forEach(t -> this.writesPerTable.put(t, new HashMap<>()));
    }

    public boolean hasConflict(Map<String, K> ws, long ts) {
        if (ts <= lowWaterMark) {
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

    public void transactionStarted(Set<String> tables, long ts) {
        for (String table : tables) {
            this.runningTransactionsPerTable.putIfAbsent(table, new ConcurrentHashMap<>());
            this.runningTransactionsPerTable.get(table)
                .compute(ts, (k,v) -> {
                    if (v == null)
                        v = 0;
                    return ++v;
                });
        }
    }

    public void shutDownLocalStartedTransaction(Set<String> tables, long ts) {
        tables.forEach(table -> this.runningTransactionsPerTable.get(table).computeIfPresent(ts, (k, v) -> --v));
    }

    //Commit also increases current timestamp
    public void commit(Map<String, K> ws) {
        ws.forEach((table, writeSet) -> {
            this.writesPerTable.putIfAbsent(table, new HashMap<>());
            this.writesPerTable.get(table).put(this.timestamp, writeSet);
        });
        try {
            rwl.writeLock().lock();
            this.timestamp++;
        }finally {
            rwl.writeLock().unlock();
        }
    }

    public long getSafeToDeleteTimestamp(){
        long minimumTimestampToDelete = Long.MAX_VALUE;
        for(ConcurrentHashMap<Long, Integer> running : this.runningTransactionsPerTable.values()){
            long mt = lowWaterMark;
            for(Map.Entry<Long, Integer> entry : running.entrySet()){
                int numTR = entry.getValue();
                // mal encontre um que não esteja a 0 não considera os restantes que possam estar a 0
                if(numTR != 0)
                    break;
                mt = numTR;
            }
            if(mt < minimumTimestampToDelete)
                minimumTimestampToDelete = mt;
        }
        return minimumTimestampToDelete;
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

    public HashMap<String, HashMap<Long, WriteSet<V>>> getWriteSetsByTimestamp(long lowerBound){
        HashMap<String, HashMap<Long, WriteSet<V>>> response = new HashMap<>();
        for(String table : this.writesPerTable.keySet()){
            response.put(table, new HashMap<>());
            for(Map.Entry<Long, WriteSet<V>> e : this.writesPerTable.get(table).entrySet()){
                if (e.getKey() > lowerBound)
                    response.get(table).put(e.getKey(), e.getValue());
            }
        }
        return response;
    }


    public long getTimestamp(){
        try {
            rwl.readLock().lock();
            return this.timestamp;
        }finally {
            rwl.readLock().unlock();
        }
    }

    public long getLowWaterMark() {
        return this.lowWaterMark;
    }

    public HashMap<String, HashMap<Long, WriteSet<V>>> getWritesPerTable(){
        return this.writesPerTable;
    }

    public HashMap<String, ConcurrentHashMap<Long, Integer>> getRunningTransactionsPerTable() {
        return runningTransactionsPerTable;
    }
}
