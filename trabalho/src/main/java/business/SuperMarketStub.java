package business;

import business.order.Order;
import business.order.OrderImpl;
import business.product.Product;
import business.product.ProductImpl;
import client.MessagingService;
import client.message.*;
import io.atomix.utils.net.Address;
import middleware.message.ContentMessage;
import middleware.message.Message;

import java.util.*;

public class SuperMarketStub implements SuperMarket {

    private MessagingService ms;

    public SuperMarketStub(int myPort, Address primaryServer){
        this.ms = new MessagingService(myPort, primaryServer);
    }

    @Override
    public boolean addCustomer(String customer) throws Exception {
        Message msg = new AddCostumerMessage(customer);
        return ms.<ContentMessage<Boolean>>sendAndReceive(msg).getBody();
    }

    @Override
    public boolean resetOrder(String customer) throws Exception {
        Message msg = new ResetOrderMessage(customer);
        return ms.<ContentMessage<Boolean>>sendAndReceive(msg).getBody();
    }

    @Override
    public boolean finishOrder(String customer) throws Exception {
        Message msg = new FinishOrderMessage(customer);
        return ms.<ContentMessage<Boolean>>sendAndReceive(msg).getBody();
    }

    @Override
    public boolean addProduct(String customer, String product, int amount) throws Exception {
        Message msg = new AddProductMessage(customer, product, amount);
        return ms.<ContentMessage<Boolean>>sendAndReceive(msg).getBody();
    }

    @Override
    public Map<Product, Integer> getCurrentOrderProducts(String customer) throws Exception {
        Message msg = new GetOrderMessage(customer);
        return ms.<ContentMessage<HashMap<Product,Integer>>>sendAndReceive(msg).getBody();
    }

    @Override
    public ArrayList<Order> getHistory(String customer) throws Exception {
        Message msg = new GetHistoryMessage(customer);
        return ms.<ContentMessage<ArrayList<Order>>>sendAndReceive(msg).getBody();
    }

    @Override
    public ArrayList<Product> getCatalogProducts() throws Exception {
        Message msg = new GetCatalogProducts();
        return ms.<ContentMessage<ArrayList<Product>>>sendAndReceive(msg).getBody();
    }
}
