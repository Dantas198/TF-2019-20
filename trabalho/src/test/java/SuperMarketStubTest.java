import business.SuperMarket;
import business.SuperMarketImpl;
import business.SuperMarketStub;
import org.junit.Test;
import server.GandaGotaServer;

public class SuperMarketStubTest {
    private GandaGotaServer server;

    public SuperMarketStubTest() throws Exception{
        server = new GandaGotaServer(4803, "1");
        server.start();
    }

    @Test
    public void addCustomerTest(){

    }

    @Test
    public void getOrderTest(){

    }
}
