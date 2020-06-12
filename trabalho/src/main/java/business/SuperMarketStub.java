package business;

import business.order.Order;
import business.product.Product;
import client.MessagingService;
import client.message.*;
import io.atomix.utils.net.Address;
import middleware.message.ContentMessage;
import middleware.message.Message;

import java.util.*;

public class SuperMarketStub implements SuperMarket {

    private MessagingService ms;

    public SuperMarketStub(int myPort, List<Address> servers){
        System.out.println(servers);
        this.ms = new MessagingService(myPort, servers);
    }

    @Override
    public boolean addCustomer(String customer) {
        try {
            Message msg = new AddCustomerMessage(customer);
            return ms.<ContentMessage<Boolean>>sendAndReceive(msg).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean resetOrder(String customer) {
        try {
            Message msg = new ResetOrderMessage(customer);
            return ms.<ContentMessage<Boolean>>sendAndReceive(msg).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean finishOrder(String customer) {
        try {
            Message msg = new FinishOrderMessage(customer);
            return ms.<ContentMessage<Boolean>>sendAndReceive(msg).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean addProductToOrder(String customer, String product, int amount) {
        try {
            Message msg = new AddProductMessage(customer, product, amount);
            return ms.<ContentMessage<Boolean>>sendAndReceive(msg).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map<Product, Integer> getCurrentOrderProducts(String customer) {
        try {
            Message msg = new GetOrderMessage(customer);
            return ms.<ContentMessage<HashMap<Product,Integer>>>sendAndReceive(msg).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ArrayList<Order> getHistory(String customer) {
        try {
            Message msg = new GetHistoryMessage(customer);
            return ms.<ContentMessage<ArrayList<Order>>>sendAndReceive(msg).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public ArrayList<Product> getCatalogProducts() {
        try {
            Message msg = new GetCatalogProductsMessage();
            return ms.<ContentMessage<ArrayList<Product>>>sendAndReceive(msg).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean updateProduct(String name, float price, String description, int stock) {
        try {
            Message msg = new UpdateProductMessage(name, price, description, stock);
            return ms.<ContentMessage<Boolean>>sendAndReceive(msg).getBody();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
