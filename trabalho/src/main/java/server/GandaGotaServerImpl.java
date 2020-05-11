package server;

import business.SuperMarket;
import business.SuperMarketImpl;
import business.product.Product;
import client.bodies.FinishOrderBody;
import client.message.AddCostumerMessage;
import client.message.FinishOrderMessage;
import client.message.GetCatalogProducts;
import client.message.GetHistoryMessage;
import middleware.ServerImpl;
import middleware.Server;
import middleware.message.ContentMessage;
import middleware.message.ErrorMessage;
import middleware.message.Message;

import java.util.ArrayList;
import java.util.Map;

public class GandaGotaServerImpl extends ServerImpl<SuperMarket> {

    private SuperMarket superMarket;

    public GandaGotaServerImpl(int port, String privateName) {
        super(port, privateName);
        this.superMarket = new SuperMarketImpl();
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
            } else if(message instanceof FinishOrderMessage){
                FinishOrderBody body = ((FinishOrderMessage) message).getBody();
                Map<Product, Integer> products = body.getOrder().getProducts();
                for(Product prod : products.keySet()){
                    superMarket.addProduct(body.getCustomer(), prod.getName(), products.get(prod));
                }
                return new ContentMessage<>(superMarket.finishOrder(body.getCustomer()));
            }
        } catch (Exception e){
            return new ErrorMessage(e).from(message);
        }
        return new ErrorMessage(new Exception("Not function for message " + message.getId() + ": " + message));
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
        Server server = new GandaGotaServerImpl(4803, "2");
        server.start();
    }
}
