package middleware.certifier;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Class that deals with certification logic.
 */

public class Certifier implements Serializable {

    //Last garbage collected timestamp limit
    private long lowWaterMark;

    //Holds current global timestamp
    private long timestamp;

    private ReadWriteLock rwl;

    //Stores running transaction for each table and each timestamp
    private final HashMap<String, ConcurrentHashMap<Long, Integer>> runningTransactionsPerTable;

    //Stores the changes commited on a certain timestamp
    private final HashMap<String, HashMap<Long, OperationalSets>> writesPerTable;

    public Certifier(){
        this.lowWaterMark = -1;
        this.timestamp = 0;
        this.runningTransactionsPerTable = new HashMap<>();
        this.writesPerTable = new HashMap<>();
        this.rwl = new ReentrantReadWriteLock();
    }

    public void addState(HashMap<String, HashMap<Long, OperationalSets>> c){
        c.forEach((table,v) -> v.forEach((ts, ws) -> {
            this.writesPerTable.putIfAbsent(table, new HashMap<>());
            this.writesPerTable.get(table).put(ts, ws);
            this.runningTransactionsPerTable.putIfAbsent(table, new ConcurrentHashMap<>());
            this.runningTransactionsPerTable.get(table).put(ts, 0);
        }));
    }

    
    public boolean isWritable(Map<String, OperationalSets> sets, long ts) {
        if (ts <= lowWaterMark) {
            System.out.println("Certifier: old timestamp arrived");
            return false;
        }

        for(Map.Entry<String, OperationalSets> entry : sets.entrySet()){
            HashMap<Long, OperationalSets> oldSets = writesPerTable.get(entry.getKey());
            for (long i = ts; i < this.timestamp; i++) {
                if (entry.getValue().intersect(oldSets.get(i)))
                    return false;
            }
        }
        return true;
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
    public long commit(Map<String, OperationalSets> sets) {
        sets.forEach((table, set) -> {
            this.writesPerTable.putIfAbsent(table, new HashMap<>());
            this.writesPerTable.get(table).put(this.timestamp, set);
        });
        try {
            rwl.writeLock().lock();
            this.timestamp++;
            return this.timestamp;
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

    public HashMap<String, HashMap<Long, OperationalSets>> getWriteSetsByTimestamp(long lowerBound){
        HashMap<String, HashMap<Long, OperationalSets>> response = new HashMap<>();
        for(String table : this.writesPerTable.keySet()){
            response.put(table, new HashMap<>());
            for(Map.Entry<Long, OperationalSets> e : this.writesPerTable.get(table).entrySet()){
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

    public HashMap<String, HashMap<Long, OperationalSets>> getWritesPerTable(){
        return this.writesPerTable;
    }

    public void setLowWaterMark(long lowWaterMark) {
        try {
            rwl.readLock().lock();
            this.lowWaterMark = lowWaterMark;
        }finally {
            rwl.readLock().unlock();
        }
    }

    public void setTimestamp(long timestamp) {
        try {
            rwl.readLock().lock();
            this.timestamp = timestamp;
        }finally {
            rwl.readLock().unlock();
        }
    }
}
