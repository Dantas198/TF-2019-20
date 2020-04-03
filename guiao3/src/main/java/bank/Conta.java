package bank;

import java.util.concurrent.ExecutionException;

public interface Conta {
    int saldo() throws ExecutionException, InterruptedException;
    boolean mov(int q) throws ExecutionException, InterruptedException;
}
