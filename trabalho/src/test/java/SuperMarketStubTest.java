import business.SuperMarket;
import business.SuperMarketImpl;
import business.SuperMarketStub;
import business.product.Product;
import io.atomix.utils.net.Address;
import org.junit.Test;
import server.GandaGotaServer;

import java.util.ArrayList;

public class SuperMarketStubTest {
    private GandaGotaServer server;

    public SuperMarketStubTest() throws Exception{
        server = new GandaGotaServer(4803, "1");
        new Thread(() ->{
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    @Test
    public void addCustomerTest() throws Exception {
        SuperMarket stub = new SuperMarketStub(8888, Address.from("localhost", 7777));
        ArrayList<Product> products = new ArrayList<>(stub.getCatalogProducts());
        System.out.println(products);
    }

}
