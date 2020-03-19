import java.io.Serializable;

public class TransferMessage implements Serializable {
    int sender;
    ContaImpl conta;

    public TransferMessage(int sender, ContaImpl conta){
        this.sender = sender;
        this.conta = conta;
    }

    @Override
    public String toString() {
        return "TransferMessage{" +
                "sender" + sender +
                "conta=" + conta.toString() +
                '}';
    }
}
