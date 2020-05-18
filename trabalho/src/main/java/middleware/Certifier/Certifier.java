package middleware.Certifier;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    public boolean hasConflict(BitWriteSet ws, List<String> tables, long ts) throws NoTableDefinedException {
        for(String table : tables){
            LinkedHashMap<Long, Round> writes = writesPerTable.get(table);
            if(writes == null)
                throw new NoTableDefinedException("No tables defined with that name");
            for (long i = ts; i < timestamp; i++) {
                BitWriteSet set = writes.get(i).getWriteSet();
                System.out.println(i);
                    if(set.intersects(ws)) {
                        return true;
                    }
                }
        }
       return false;
    }

    public synchronized void commitLocalStartedTransaction(BitWriteSet ws, List<String> tables, long ts, String id) throws NoTableDefinedException {
        for(String table : tables){
            LinkedHashMap<Long, Round> writes = this.writesPerTable.get(table);
            if(writes == null)
                throw new NoTableDefinedException("No tables defined with that name");
            writes.get(this.timestamp).addCommit(ws);
            writes.get(ts).removeStarted(id);
            this.timestamp++;
        }
    }

    public synchronized void commit(BitWriteSet ws, List<String> tables) throws NoTableDefinedException {
        //Commit also increases current timestamp
        for(String table : tables){
            LinkedHashMap<Long, Round> writes = this.writesPerTable.get(table);
            if(writes == null)
                throw new NoTableDefinedException("No tables defined with that name");

            Round r = writes.get(this.timestamp);
            if(r == null){
                r = new Round();
                r.addCommit(ws);
                writes.put(this.timestamp, r);
            }else{
                r.addCommit(ws);
            }
            this.timestamp++;
        }
    }

    public synchronized long getTimestamp(){
        return this.timestamp;
    }

    public synchronized void transactionStarted(List<String> tables, long ts, String id) throws NoTableDefinedException {
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
