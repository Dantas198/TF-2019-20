import bank.ContaStub;
import spread.SpreadException;

import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class Main {
    public static void main(String[] args) throws ExecutionException, InterruptedException, SpreadException, UnknownHostException {
        Random r = new Random();
        ContaStub cs = new ContaStub(20000);
        cs.start();
        int saldoLocal = cs.saldo();
        for(int i = 0; i< 1000; i++){
            int q = r.nextInt(200) - 100;
            System.out.println("iter: " + i);
            if(cs.mov(q)){
                saldoLocal+=q;
                System.out.println("saldo remoto: " +  cs.saldo());
                System.out.println("saldo local: " + saldoLocal);
                System.out.println("---------------------------");
            }
        }
    }
}
