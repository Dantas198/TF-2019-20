package server;

import business.SuperMarket;
import business.SuperMarketImpl;
import middleware.PassiveReplicationServer;

import java.io.Serializable;

public class GandaGotaServer extends PassiveReplicationServer<SuperMarket> {

    private SuperMarket superMarket;

    public GandaGotaServer(int port, String privateName) {
        super(port, privateName);
        this.superMarket = new SuperMarketImpl();
    }

    @Override
    public Serializable handleMessage(Serializable message) {
        return null;
    }

    @Override
    public SuperMarket getState() {
        return superMarket;
    }

    @Override
    public void setState(SuperMarket superMarket) {
        this.superMarket = superMarket;
    }
}
