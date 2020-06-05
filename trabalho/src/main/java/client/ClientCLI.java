package client;

import business.SuperMarket;
import business.SuperMarketStub;
import client.autocli.AutoCLI;
import io.atomix.utils.net.Address;

import java.util.LinkedList;
import java.util.List;

public class ClientCLI {

    public static void main(String[] args) throws Exception {

        Address primaryServer = Address.from(6000);
        List<Address> addresses = new LinkedList<>();
        addresses.add(primaryServer);
        int myPort = 5555;
        SuperMarket sm = new SuperMarketStub(myPort, addresses);

        AutoCLI<SuperMarket> cli = new AutoCLI<>(SuperMarket.class, sm);

        try {
            cli.startInputLoop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
