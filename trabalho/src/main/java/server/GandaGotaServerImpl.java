package server;

import business.SuperMarket;
import business.SuperMarketImpl;
import business.product.Product;
import client.bodies.FinishOrderBody;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class GandaGotaServerImpl extends ServerImpl<ArrayList<String>> {

    private SuperMarket superMarket;

    public GandaGotaServerImpl(int spreadPort, String privateName, int atomixPort) {
        super(spreadPort, privateName, atomixPort);
        //TODO tmax não à sorte poderá aumentar/diminuir consoante a quantidade de aborts
        this.superMarket = new SuperMarketImpl();
    }

    //TODO classe á parte?
    @Override
    public Message handleMessage(Message message) {
        try{
            if(message instanceof AddCostumerMessage){
                String customer = ((AddCostumerMessage) message).getBody();
                return new ContentMessage<>(superMarket.addCustomer(customer));
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
        //TODO PRE-PROCESSAMENTO
        /*
        FinishOrderBody body = ((FinishOrderMessage) message).getBody();
        Map<Product, Integer> products = body.getOrder().getProducts();
        for(Product prod : products.keySet()){
            superMarket.addProduct(body.getCustomer(), prod.getName(), products.get(prod));
        }
        // return new ContentMessage<>(superMarket.finishOrder(body.getCustomer()));
        return new FinishOrderPreProcessedMessage(null, null);
         */

        //Para testes
        WriteMessage<HashSet<String>> wm = (WriteMessage) message;
        BitWriteSet bws = new BitWriteSet();
        for(String s : wm.getBody()){
            bws.add(s.getBytes());
        }
        return new CertifyWriteMessage<>(bws, 1, new ArrayList<>());
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
        Server server = new GandaGotaServerImpl(4803, "2", 7779);
        server.start();
    }
}
