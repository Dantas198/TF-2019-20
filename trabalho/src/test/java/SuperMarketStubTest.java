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
        List<Address> servers = new ArrayList<>();
        servers.add(Address.from("localhost", 6000));
        this.stub = new SuperMarketStub(9999, servers);
    }

    @Test
    public void spamTest() throws Exception {
        for(int i = 0; i < 100000; i++) {
            assertTrue(stub.addCustomer(UUID.randomUUID().toString()));
        }
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

    //@Test
    public void addCustomerTest() throws Exception {
        addCustomerTest(UUID.randomUUID().toString());
    }


    //@Test
    private void addCustomerTest(String customerName) throws Exception {
        boolean added = stub.addCustomer(customerName);
        System.out.println(customerName);
        assertTrue("Response of adding a costumer should not be null" , added);
    }

    @Test
    public void addProductTest() throws Exception {
        String customerName = UUID.randomUUID().toString();
        stub.addCustomer(customerName);
        boolean added = stub.addProductToOrder(customerName, "Queijo", 1);
        if(added) {
            stub.getCurrentOrderProducts(customerName);
            Iterator<Map.Entry<Product, Integer>> iter = stub.getCurrentOrderProducts(customerName).entrySet().iterator();
            assertTrue("Should have product", iter.hasNext());
            assertEquals("Product should be 'Queijo'", iter.next().getKey().getName(), "Queijo");
        }
        assertNotNull("Response of adding a costumer should not be null" , added);
    }
}
