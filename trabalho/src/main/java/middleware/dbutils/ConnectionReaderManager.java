package middleware.dbutils;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReentrantLock;

public abstract class ConnectionReaderManager<K> {
    private final Queue<K> freeConnections;
    private Queue<CompletableFuture<K>> readersWaiting;

    //TODO o lock vai depender de como usamos a classe. Ver melhor depois
    private ReentrantLock rl;
    private final int maxWriteConnectionNumber;
    private int currentConnections;

    public ConnectionReaderManager(int maxConnectionNumber){
        this.freeConnections = new ConcurrentLinkedQueue<>();
        this.readersWaiting = new ConcurrentLinkedQueue<>();
        this.rl = new ReentrantLock();
        this.maxWriteConnectionNumber = maxConnectionNumber;
        this.currentConnections = 0;
    }

    /**
     *
     * @return new connection to add to structures
     */
    public abstract K produceConnection();

    public CompletableFuture<K> assignRequest(){
        try {
            rl.lock();
            if (freeConnections.isEmpty() && currentConnections == maxWriteConnectionNumber) {
                CompletableFuture<K> cf = new CompletableFuture<>();
                readersWaiting.add(cf);
                return cf;
            }
            K offered_connection;
            if (freeConnections.isEmpty()) {
                offered_connection = produceConnection();
                currentConnections++;
            }else {
                offered_connection = freeConnections.poll();
            }

            return CompletableFuture.completedFuture(offered_connection);

        } finally {
            rl.unlock();
        }
    }

    public void releaseConnection(K connection){
        try{
            rl.lock();
            if (!readersWaiting.isEmpty())
                readersWaiting.poll().complete(connection);
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
}
