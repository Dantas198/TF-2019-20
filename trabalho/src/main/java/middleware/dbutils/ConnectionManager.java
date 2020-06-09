package middleware.dbutils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ConnectionManager<K> {
    private final Queue<K> freeConnections;
    private final Queue<CompletableFuture<K>> writesOnWait;

    //TODO o lock vai depender de como usamos a classe. Ver melhor depois
    private ReentrantLock rl;
    private final int maxConnectionNumber;
    private int currentConnections;

    private List<K> readersPool;

    public ConnectionManager(int maxConnectionNumber){
        this.freeConnections = new LinkedList<>();
        this.writesOnWait = new LinkedList<>();
        this.rl = new ReentrantLock();
        this.maxConnectionNumber = maxConnectionNumber;
        this.currentConnections = 0;
        this.readersPool = new ArrayList<>(1);
        this.readersPool.add(produceConnection());
    }

    public ConnectionManager(int maxConnectionNumber, int readersPoolSize){
        this(maxConnectionNumber);
        readersPool = new ArrayList<>();
        for(int i = 0; i < readersPoolSize; i++)
            readersPool.add(produceConnection());
    }

    /**
     *
     * @return new connection to add to structures
     */
    public abstract K produceConnection();

    public CompletableFuture<K> assignWriteRequest(){
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

            return CompletableFuture.completedFuture(offered_connection);

        } finally {
            rl.unlock();
        }
    }

    public void releaseConnection(K connection){
        try{
            rl.lock();
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
        Random rand = new Random();
        return readersPool.get(rand.nextInt(readersPool.size()));
    }
}
