package messages;

import io.atomix.utils.net.Address;

import java.io.Serializable;

public class TransferStateMessage implements Serializable {
    public ReplyMessage lastReply;
    public int client;
    public int saldo;

    public TransferStateMessage(int saldo, Address client, ReplyMessage lastReply){
        this.saldo = saldo;
        //address do atomix não é serializavel
        this.client = client.port();
        this.lastReply = lastReply;
    }

    @Override
    public String toString() {
        return "TransferStateMessage{" +
                "saldo=" + saldo +
                '}';
    }
}
