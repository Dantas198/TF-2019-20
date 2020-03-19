import java.io.Serializable;

public class ReplyMessage implements Serializable{
    int reqId; //id da msg de pedido
    int q;
    boolean b;
    int type;
    //1 -> saldo
    //2 -> movimento

    public ReplyMessage(int reqId, int q){
        this.reqId = reqId;
        this.q = q;
        type=1;
    }

    public ReplyMessage(int reqId, boolean b){
        this.reqId = reqId;
        this.b = b;
        type=2;
    }

    @Override
    public String toString() {
        return "ReplyMessage{" +
                "reqId=" + reqId +
                "q=" + q +
                ", b=" + b +
                ", type=" + type +
                '}';
    }
}
