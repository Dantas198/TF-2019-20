package server;

import business.SuperMarketImpl;
import business.product.Product;
import client.bodies.AddProductBody;
import client.message.*;
import middleware.Certifier.BitWriteSet;
import middleware.Server;
import middleware.ServerImpl;
import middleware.message.ContentMessage;
import middleware.message.ErrorMessage;
import middleware.message.Message;
import middleware.message.WriteMessage;
import middleware.message.replication.CertifyWriteMessage;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GandaGotaServerImpl extends ServerImpl<ArrayList<String>> {

    private SuperMarketImpl superMarket;

    public GandaGotaServerImpl(int spreadPort, String privateName, int atomixPort) throws SQLException {
        super(spreadPort, privateName, atomixPort);
        //TODO tmax não poderá aumentar/diminuir consoante a quantidade de aborts
        this.superMarket = new SuperMarketImpl(privateName);
    }

    //TODO classe á parte?
    @Override
    public Message handleMessage(Message message) {
        try {
            if(message instanceof AddCostumerMessage) {
                String customer = ((AddCostumerMessage) message).getBody();
                return new ContentMessage<>(superMarket.addCustomer(customer));
            } else if(message instanceof GetOrderMessage) {
                String customer = ((GetOrderMessage) message).getBody();
                return new ContentMessage<>((HashMap<Product, Integer>) superMarket.getCurrentOrderProducts(customer));
            } else if(message instanceof AddProductMessage) {
                AddProductBody body = ((AddProductMessage) message).getBody();
                String customer = body.getCustomer();
                String product = body.getProduct();
                int amount = body.getAmount();
                return new ContentMessage<>(superMarket.addProduct(customer, product, amount));
            } else if(message instanceof FinishOrderMessage) {
                return new ErrorMessage(new Error("Invalid non-write Message")).from(message);
            } else if(message instanceof ResetOrderMessage) {
                String customer = ((ResetOrderMessage) message).getBody();
                return new ContentMessage<>(superMarket.resetOrder(customer));
            } else if(message instanceof GetCatalogProducts){
                return new ContentMessage<>(new ArrayList<>(superMarket.getCatalogProducts()));
            } else if(message instanceof GetHistoryMessage) {
                return new ContentMessage<>(new ArrayList<>(superMarket.getHistory(((GetHistoryMessage) message).getBody())));
            }
        } catch (Exception e){
            return new ErrorMessage(e).from(message);
        }
        return new ErrorMessage(new Exception("Not function for message " + message.getId() + ": " + message));
    }

    @Override
    public CertifyWriteMessage<?> preprocessMessage(Message message){
        Map<String, BitWriteSet> writeSets = new HashMap<>();
        if (message instanceof FinishOrderMessage) {
            String customer = ((FinishOrderMessage) message).getBody();
            superMarket.finishOrder(customer, writeSets);
        }
        return new CertifyWriteMessage<>(writeSets, 1);
    }

    @Override
    public void updateStateFromCommitedWrite(CertifyWriteMessage<?> message) {
        //TODO
        //TESTE
        System.out.println("Server : " + this.getPrivateName() + " update state from commit");
        int state = (Integer) message.getState();
    }

    @Override
    public void commit(){
        //TODO
        System.out.println("Server : " + this.getPrivateName() + " commit");
    }

    @Override
    public void rollback(){
        //TODO
        System.out.println("Server : " + this.getPrivateName() + " rollback");
    }

    @Override
    public ArrayList<String> getState() {
        return null;
    }

    @Override
    public void setState(ArrayList<String> queries) {
        try {
            Connection c = DriverManager.getConnection("jdbc:hsqldb:file:testdb;shutdown=true;hsqldb.sqllog=2", "", "");
            for(String query : queries) {
                c.prepareStatement(query).execute();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Server server = new GandaGotaServerImpl(4803, "5", 7775);
        server.start();
    }
}
