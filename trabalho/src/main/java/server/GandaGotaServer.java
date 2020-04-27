package server;

import business.SuperMarket;
import business.SuperMarketImpl;
import client.message.AddCustumer;
import client.message.GetProductsMessage;
import io.atomix.utils.net.Address;
import middleware.PassiveReplicationServer;
import middleware.Server;

import java.io.Serializable;

public class GandaGotaServer extends PassiveReplicationServer<SuperMarket> {

    private SuperMarket superMarket;

    public GandaGotaServer(int port, String privateName) {
        super(port, privateName);
        this.superMarket = new SuperMarketImpl();
    }

    @Override
    public Serializable handleMessage(Serializable message) {
        if(message instanceof AddCustumer){
            if(((AddCustumer) message).getBody() instanceof String){
                String customer = (String) ((AddCustumer) message).getBody();
                return superMarket.addCustomer(customer);
            }
        } else if(message instanceof GetProductsMessage){
            return superMarket.getCatalogProducts();
        }
        return false;
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
