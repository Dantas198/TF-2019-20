package middleware.Certifier;

import java.util.*;

/**
 * Class that deals with certification logic.
 */

//TODO garbage collection
public class Certifier {
    //Holds current global timestamp
    private long timestamp;
    //Stores the changes commited on a certain timestamp
    private HashMap<String, LinkedHashMap<Long, Round>> writesPerTable;


    //TODO arranjar controlo de concorrência

    public Certifier(){
        this.timestamp = 0;
        this.writesPerTable = new LinkedHashMap<>();
    }

    public void addTables(List<String> tables){
        for(String table : tables)
            this.writesPerTable.put(table, new LinkedHashMap<>());
    }

    public boolean hasConflict(Map<String, BitWriteSet> ws, long ts) throws NoTableDefinedException {
        for(Map.Entry<String, BitWriteSet> entry : ws.entrySet()){
            String table = entry.getKey();
            LinkedHashMap<Long, Round> writes = writesPerTable.get(table);
            if(writes == null)
                throw new NoTableDefinedException("No tables defined with that name");
            for (long i = ts; i < timestamp; i++) {
                BitWriteSet set = writes.get(i).getWriteSet();
                System.out.println(i);
                BitWriteSet bws = entry.getValue();
                if(set.intersects(bws)) {
                    return true;
                }
            }
        }
       return false;
    }

    public synchronized void commitLocalStartedTransaction(Map<String, BitWriteSet> ws, long ts, String id) throws NoTableDefinedException {
        for(Map.Entry<String, BitWriteSet> entry : ws.entrySet()){
            String table = entry.getKey();
            LinkedHashMap<Long, Round> writes = this.writesPerTable.get(table);
            if(writes == null)
                throw new NoTableDefinedException("No tables defined with that name");
            BitWriteSet bws = entry.getValue();
            writes.get(this.timestamp).addCommit(bws);
            writes.get(ts).removeStarted(id);
            this.timestamp++;
        }
    }

    public synchronized void commit(Map<String, BitWriteSet> ws) throws NoTableDefinedException {
        //Commit also increases current timestamp
        for(Map.Entry<String, BitWriteSet> entry : ws.entrySet()){
            String table = entry.getKey();
            LinkedHashMap<Long, Round> writes = this.writesPerTable.get(table);
            if(writes == null)
                throw new NoTableDefinedException("No tables defined with that name");

            Round r = writes.get(this.timestamp);
            BitWriteSet bws = entry.getValue();
            if(r == null){
                r = new Round();
                r.addCommit(bws);
                writes.put(this.timestamp, r);
            }else{
                r.addCommit(bws);
            }
            this.timestamp++;
        }
    }

    public synchronized long getTimestamp(){
        return this.timestamp;
    }

    public synchronized void transactionStarted(Set<String> tables, long ts, String id) throws NoTableDefinedException {
        for (String table : tables) {
            LinkedHashMap<Long, Round> writes = this.writesPerTable.get(table);
            if (writes == null)
                throw new NoTableDefinedException("No tables defined with that name");
            Round r = writes.get(ts);
            if (r == null) {
                r = new Round();
                r.addStarted(id);
                writes.put(ts, r);
            } else
                r.addStarted(id);
        }
    }

    //TODO não suporta clientes lentos. Para suportar -> Force GC -> Round com Set de transactionId
    public synchronized long getSafeToDeleteTimestamp(){
        long maxTimestampToDelete = Long.MAX_VALUE;
        for(LinkedHashMap<Long, Round> writes : this.writesPerTable.values()){
            long minimumTimestamp = Long.MAX_VALUE;
            for(Map.Entry<Long, Round> entry : writes.entrySet()){
                int numTR = entry.getValue().getNumTransactionRunning();
                // mal encontre um que não esteja a 0 não considera os restantes que possam estar a 0
                if(numTR != 0)
                    break;
                minimumTimestamp = numTR;
            }
            if(minimumTimestamp < maxTimestampToDelete)
                maxTimestampToDelete = minimumTimestamp;
        }
        return maxTimestampToDelete;
    }


}
