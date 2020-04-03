package messages;

import java.io.Serializable;

public class TransferStateMessage implements Serializable {
    public int saldo;

    public TransferStateMessage(int saldo){
        this.saldo = saldo;
    }

    @Override
    public String toString() {
        return "TransferStateMessage{" +
                "saldo=" + saldo +
                '}';
    }
}
