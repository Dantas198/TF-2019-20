package server;

import business.SuperMarket;
import business.SuperMarketImpl;
import business.product.Product;
import client.bodies.FinishOrderBody;
import client.message.*;
import middleware.Server;
import middleware.ServerImpl;
import middleware.message.ContentMessage;
import middleware.message.ErrorMessage;
import middleware.message.Message;
import middleware.message.replication.CertifyWriteMessage;

import java.util.ArrayList;
import java.util.Map;

public class GandaGotaServerImpl extends ServerImpl<SuperMarket> {

    private SuperMarket superMarket;

    public GandaGotaServerImpl(int spreadPort, String privateName, int atomixPort) {
        super(spreadPort, privateName, atomixPort);
        //TODO tmax não à sorte poderá aumentar/diminuir consoante a quantidade de aborts
        this.superMarket = new SuperMarketImpl(1000);
    }

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
    public FinishOrderPreProcessedMessage preprocessMessage(Message message){
        //TODO PRE-PROCESSAMENTO
        FinishOrderBody body = ((FinishOrderMessage) message).getBody();
        Map<Product, Integer> products = body.getOrder().getProducts();
        for(Product prod : products.keySet()){
            superMarket.addProduct(body.getCustomer(), prod.getName(), products.get(prod));
        }
        // return new ContentMessage<>(superMarket.finishOrder(body.getCustomer()));
        return new FinishOrderPreProcessedMessage(null, null);
    }

    @Override
    public void updateStateFromCommitedWrite(CertifyWriteMessage<?> message) {
        //TODO
    }

    @Override
    public void commit(){
        //TODO
    }

    @Override
    public void rollback(){
        //TODO
    }

    @Override
    public SuperMarket getState() {
        System.out.println("Retrieving state");
        return superMarket;
    }

    @Override
    public void setState(SuperMarket superMarket) {
        System.out.println("Setting state");
        this.superMarket = superMarket;
    }

    public static void main(String[] args) throws Exception {
        Server server = new GandaGotaServerImpl(4803, "2", 7777);
        server.start();
    }
}
