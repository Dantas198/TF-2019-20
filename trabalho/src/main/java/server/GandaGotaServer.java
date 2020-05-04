package server;

import business.SuperMarket;
import business.SuperMarketImpl;
import business.product.Product;
import business.product.ProductImpl;
import client.message.AddCostumerMessage;
import client.message.GetCatalogProducts;
import client.message.GetProductsMessage;
import middleware.PassiveReplicationServer;
import middleware.Server;
import middleware.message.ContentMessage;
import middleware.message.Message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class GandaGotaServer extends PassiveReplicationServer<SuperMarket> {

    private SuperMarket superMarket;

    public GandaGotaServer(int port, String privateName) {
        super(port, privateName);
        this.superMarket = new SuperMarketImpl();
    }

    @Override
    public Message handleMessage(Message message) {
        try{
            if(message instanceof AddCostumerMessage){
                String customer = ((AddCostumerMessage) message).getBody();
                return new ContentMessage<>(superMarket.addCustomer(customer));
            } else if(message instanceof GetProductsMessage){
                return new ContentMessage<>(new ArrayList<>(superMarket.getCatalogProducts()));
            } else if(message instanceof GetCatalogProducts){
                return new ContentMessage<>(new ArrayList<>(superMarket.getCatalogProducts()));
            }
        } catch (Exception e){
            return new ContentMessage<>(null).from(message);
        }
        return new ContentMessage<>(null).from(message);
    }

    @Override
    public SuperMarket getState() {
        return superMarket;
    }

    @Override
    public void setState(SuperMarket superMarket) {
        this.superMarket = superMarket;
    }

    public static void main(String[] args) throws Exception {
        Server server = new GandaGotaServer(4803, "1");
        server.start();
    }
}
