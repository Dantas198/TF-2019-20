package messages;

import java.io.Serializable;

public class RequestMessage implements Serializable{
    public int id;
    public int q = 0;
    public int type;
    //1 -> saldo
    //2 -> movimento

    public RequestMessage(int id){
        this.id = id;
        this.type = 1;
    }

    public RequestMessage(int id, int q){
        this.id = id;
        this.q = q;
        this.type = 2;
    }

    @Override
    public String toString() {
        return "messages.RequestMessage{" +
                "id= " + id +
                "q=" + q +
                ", type=" + type +
                '}';
    }
}
