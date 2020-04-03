package messages;

import java.io.Serializable;
import java.util.Objects;

public class ReplyMessage implements Serializable{
    public int reqId; //id da msg de pedido
    public int q;
    public boolean b;
    public int type;
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

    public boolean answerToSameRequest(RequestMessage reqm) {
        if(reqm == null) return false;
        return reqId == reqm.id;
    }


    @Override
    public String toString() {
        return "messages.ReplyMessage{" +
                "reqId=" + reqId +
                "q=" + q +
                ", b=" + b +
                ", type=" + type +
                '}';
    }
}
