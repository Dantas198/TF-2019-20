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
import middleware.message.TransactionMessage;
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


    @Override
    public Message handleMessage(Message message) {
        try {
            if(message instanceof AddCustomerMessage) {
                String customer = ((AddCustomerMessage) message).getBody();
                return new ContentMessage<>(superMarket.addCustomer(customer));
            }
            if(message instanceof GetOrderMessage) {
                String customer = ((GetOrderMessage) message).getBody();
                return new ContentMessage<>((HashMap<Product, Integer>) superMarket.getCurrentOrderProducts(customer));
            }
            if(message instanceof AddProductMessage) {
                AddProductBody body = ((AddProductMessage) message).getBody();
                String customer = body.getCustomer();
                String product = body.getProduct();
                int amount = body.getAmount();
                return new ContentMessage<>(superMarket.addProduct(customer, product, amount));
            }
            if(message instanceof ResetOrderMessage) {
                String customer = ((ResetOrderMessage) message).getBody();
                return new ContentMessage<>(superMarket.resetOrder(customer));
            }
            if(message instanceof GetCatalogProductsMessage){
                return new ContentMessage<>(new ArrayList<>(superMarket.getCatalogProducts()));
            }
            if(message instanceof GetHistoryMessage) {
                return new ContentMessage<>(new ArrayList<>(superMarket.getHistory(((GetHistoryMessage) message).getBody())));
            }
        } catch (Exception e){
            return new ErrorMessage(e).from(message);
        }
        return new ErrorMessage(new Exception("Unrecognized message " + message.getId() + ": " + message));
    }

    @Override
    public CertifyWriteMessage<?> handleTransactionMessage(TransactionMessage<?> message){
        Map<String, BitWriteSet> writeSets = new HashMap<>();
        if (message instanceof FinishOrderMessage) {
            String customer = ((FinishOrderMessage) message).getBody();
            boolean success = superMarket.finishOrder(customer, writeSets);
            if(success) System.out.println("correu bem");
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
            Connection c = DriverManager.getConnection("jdbc:hsqldb:file:db/testdb;shutdown=true;hsqldb.sqllog=2", "", "");
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
