package main.java;

import java.io.Serializable;

public class RequestMessage implements Serializable{
    int id;
    int q = 0;
    int type;
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


    public void setType(int type){
        this.type = type;
    }
    @Override
    public String toString() {
        return "RequestMessage{" +
                "id= " + id +
                "q=" + q +
                ", type=" + type +
                '}';
    }
}
