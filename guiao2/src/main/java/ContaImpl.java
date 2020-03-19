import java.io.Serializable;

public class ContaImpl implements Conta, Serializable {
    private int saldo;

    public ContaImpl(int saldo){
        this.saldo = saldo;
        System.out.println(" Update de saldo: " + saldo);
    }

    public int saldo() {
        return saldo;
    }

    public boolean mov(int q) {
        if (q < 0 && saldo + q < 0)
            return false;
        else
            saldo += q;
        return true;
    }

    @Override
    public String toString() {
        return "ContaImpl{" +
                "saldo=" + saldo +
                '}';
    }
}
