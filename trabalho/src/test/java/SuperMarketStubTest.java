import business.SuperMarket;
import business.SuperMarketStub;
import business.product.Product;
import io.atomix.utils.net.Address;
import org.junit.Test;
import server.GandaGotaServerImpl;

import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertNotNull;

public class SuperMarketStubTest {
    private GandaGotaServerImpl server;
    private SuperMarket stub;

    public SuperMarketStubTest() throws Exception{
        server = new GandaGotaServerImpl(4803, "5", 7777);
        new Thread(() ->{
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        this.stub = new SuperMarketStub(8888, Address.from("localhost", 7777));
    }

    @Test
    public void getCatalogProductsTest() throws Exception {
       // Collection<Product> products = stub.getCatalogProducts();
       // Collection<Product> serverProducts = server.getState().getCatalogProducts();
       // collectionsTest(products, serverProducts, "Products");
    }

    private <T> void collectionsTest(Collection<T> local, Collection<T> server, String collectionName) {
        boolean receivedAll = server.containsAll(local);
        assertTrue("received items that don't exist in server from collection \"" + collectionName + "\""
                , receivedAll);
        boolean sameLength = server.size() == local.size();
        assertTrue("received a different amount of products that the server has from collection \""
                + collectionName + "\"", sameLength);
    }

    @Test
    public void addCostumerTest() throws Exception {
        boolean added = stub.addCustomer("123");
        assertNotNull("Response of adding a costumer should not be null" , added);
    }
}
