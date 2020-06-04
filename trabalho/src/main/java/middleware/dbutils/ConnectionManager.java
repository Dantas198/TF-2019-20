package middleware.dbutils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;


public abstract class ConnectionManager<K> {
    private Map<String, K> writeRequestsConnections;
    private Queue<K> freeConnections;
    private Queue<CompletableFuture<K>> writesOnWait;

    //TODO o lock vai depender de como usamos a classe. Ver melhor depois
    private ReentrantLock rl;
    private final int maxConnectionNumber;
    private int currentConnections;

    //readers use writers connections or own pool
    private boolean sharedPool;
    private List<K> readersPool;

    public ConnectionManager(int maxConnectionNumber){
        this.writeRequestsConnections = new HashMap<>();
        this.freeConnections = new LinkedList<>();
        this.writesOnWait = new LinkedList<>();
        this.rl = new ReentrantLock();
        this.maxConnectionNumber = maxConnectionNumber;
        this.currentConnections = 0;
        this.sharedPool = false;
    }

    public ConnectionManager(int maxConnectionNumber, int readersPoolSize){
        this(maxConnectionNumber);
        this.sharedPool = true;
        readersPool = new ArrayList<>();
        for(int i = 0; i < readersPoolSize; i++)
            readersPool.add(produceConnection());
    }

    /**
     *
     * @return new connection to add to structures
     */
    public abstract K produceConnection();

    public CompletableFuture<K> assignWriteRequest(String writeMsgId){
        try {
            rl.lock();
            if (freeConnections.isEmpty() && currentConnections == maxConnectionNumber) {
                CompletableFuture<K> cf = new CompletableFuture<>();
                writesOnWait.add(cf);
                return cf;
            }
            K offered_connection;
            if (freeConnections.isEmpty()) {
                offered_connection = produceConnection();
                currentConnections++;
            }else
                offered_connection = freeConnections.poll();

            writeRequestsConnections.put(writeMsgId, offered_connection);
            return CompletableFuture.completedFuture(offered_connection);

        } finally {
            rl.unlock();
        }
    }

    public K getAssignedConnection(String writeMsgId){
        K connection;
        try{
            rl.lock();
            connection = writeRequestsConnections.get(writeMsgId);
            return connection;
        }finally {
            rl.unlock();
        }
    }

    public void releaseConnection(String writeMsgId){
        try{
            rl.lock();
            K connection = writeRequestsConnections.remove(writeMsgId);
            if (!writesOnWait.isEmpty())
                writesOnWait.poll().complete(connection);
            else
                freeConnections.add(connection);
        }finally {
            rl.unlock();
        }
    }

    // se necessário
    public int trimConnections(int size){
        try {
            rl.lock();
            if (size > freeConnections.size())
                return -1;
            for(int i = 0; i < size; i++)
                freeConnections.poll();
            return size;
        }finally {
            rl.unlock();
        }
    }

    public K getReadConnection(){
        if(!sharedPool) {
            Random rand = new Random();
            return readersPool.get(rand.nextInt(readersPool.size()));
        }
        return getReadConnectionFromSharedPool();
    }

    private K getReadConnectionFromSharedPool(){
        try{
            K offered_connection;
            rl.lock();
            if (freeConnections.isEmpty() && writeRequestsConnections.isEmpty()){
                offered_connection = produceConnection();
            }
            else if(freeConnections.isEmpty())
                //não gosto muito
                offered_connection = writeRequestsConnections.values().iterator().next();
            else
                offered_connection = freeConnections.peek();
            return offered_connection;
        }finally {
            rl.unlock();
        }
    }

}
