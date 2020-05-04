package main.java;

import spread.*;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;


//55b8b484-fe00-4c8a-9e96-8cc2a130bf48
public class Server {
    int id;
    boolean transferring;
    ArrayList<SpreadMessage> queue;
    ContaSkel skel = new ContaSkel();
    SpreadConnection connection;

    public Server(int id) throws Exception{
        if(id == 1)
            Thread.sleep(5000);
        this.id = id;
        connection = new SpreadConnection();
        queue = new ArrayList<>();
        transferring = true;
        connection.connect(InetAddress.getByName("localhost"), 0,
                "teste" + id, false, true);

        SpreadGroup group = new SpreadGroup();
        group.join(connection, "bank");
        connection.add(new AdvancedMessageListener() {
            @Override
            public void regularMessageReceived(SpreadMessage message) {
                try {
                    Object o = message.getObject();
                    //Pedido de cliente
                    if(o instanceof RequestMessage){
                        if(transferring) {
                            queue.add(message);
                            return;
                        }
                        RequestMessage reqm = (RequestMessage) o;
                        ReplyMessage repm = skel.resolve(reqm);
                        System.out.println("Server " + id + " received " + reqm.toString());
                        System.out.println("Server " + id + " received " + repm.toString());
                        sendMsg(repm, message.getSender());
                    }
                    //Receção da transferência
                    else{
                        TransferMessage tm = (TransferMessage) o;
                        if(tm.sender == id || !transferring)
                            return;
                        System.out.println("Server " + id + " RESPOSTA DE TRANFERENCIA CHEGOU!!!");
                        System.out.println(tm.toString());
                        skel = new ContaSkel(tm.conta);
                        transferring = false;
                        for (SpreadMessage sm : queue)
                            sendMsg(sm);
                        queue.clear();
                    }
                } catch (SpreadException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void membershipMessageReceived(SpreadMessage message) {
                //se recebi a msg de membership da minha própria entrada
                TransferMessage tm = new TransferMessage(id, skel.getContaImpl());
                System.out.println("Server " + id + " " + tm.toString());
                try {
                    sendMsg(tm, message.getSender());
                } catch (SpreadException e) {
                    e.printStackTrace();
                }
            }
        });
        while(true) Thread.sleep(1000);
    }

    private void sendMsg(SpreadMessage message) throws SpreadException {
        RequestMessage reqm = (RequestMessage) message.getObject();
        ReplyMessage repm = skel.resolve(reqm);
        sendMsg(repm, message.getSender());
    }

    private <T extends Serializable> void sendMsg(T rm, SpreadGroup g) throws SpreadException {
        SpreadMessage m = new SpreadMessage();
        m.addGroup(g);
        m.setObject(rm);
        m.setReliable();
        connection.multicast(m);
    }

    public static void main(String[] args) throws InterruptedException {
        new Thread(() -> {
            try {
                new Server(1);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                new Server(2);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        new Thread(() -> {
            try {
                new Server(3);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
