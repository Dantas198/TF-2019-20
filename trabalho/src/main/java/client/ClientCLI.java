package client;

import business.SuperMarket;
import business.SuperMarketStub;
import io.atomix.utils.net.Address;

public class ClientCLI {

    public static void main(String[] args) throws Exception {

        Address primaryServer = Address.from(6666);
        int myPort = 5555;
        SuperMarket sm = new SuperMarketStub(myPort, primaryServer);

        AutoCLI<SuperMarket> cli = new AutoCLI<>(SuperMarket.class, sm);

        try {
            cli.startInputLoop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
