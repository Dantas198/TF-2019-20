package business;

import business.order.Order;
import business.order.OrderImpl;
import business.product.MessagingService;
import business.product.Product;
import business.product.ProductImpl;
import client.message.AddCostumerMessage;
import client.message.FinishOrderMessage;
import client.message.GetCatalogProducts;
import client.message.GetHistoryMessage;
import io.atomix.utils.net.Address;
import middleware.message.ContentMessage;
import middleware.message.Message;

import java.util.*;

public class SuperMarketStub implements SuperMarket {

    private String privateCustomer;
    private Order currentOrder;

    private MessagingService ms;

    public SuperMarketStub(int myPort, Address primaryServer){

        this.currentOrder = null;
        this.ms = new MessagingService(myPort, primaryServer);
    }

    @Override
    public boolean addCustomer(String customer) throws Exception {
        this.privateCustomer = customer;
        return ((ContentMessage<Boolean>) ms.getResponse(new AddCostumerMessage(customer))).getBody();
    }

    @Override
    public boolean resetOrder(String customer) {
        this.currentOrder.reset();
        return true;
    }

    @Override
    public boolean finishOrder(String customer) throws Exception {
        if(!customer.equals(privateCustomer))
            return false;
        return ((ContentMessage<Boolean>) ms.getResponse(new FinishOrderMessage(customer, currentOrder))).getBody();
    }

    @Override
    public boolean addProduct(String customer, String product, int amount) {
        if(!customer.equals(privateCustomer))
            return false;
        if(this.currentOrder == null)
            this.currentOrder = new OrderImpl();
        Product p = new ProductImpl(product);
        this.currentOrder.addProduct(p, amount);
        return true;
    }

    @Override
    public Map<Product, Integer> getCurrentOrderProducts(String customer) {
        return this.currentOrder.getProducts();
    }

    @Override
    public ArrayList<Order> getHistory(String customer) throws Exception {
        ContentMessage<ArrayList<Order>> cm = ((ContentMessage<ArrayList<Order>>)  ms.getResponse(new GetHistoryMessage(customer)));
        return cm.getBody();
    }

    @Override
    public ArrayList<Product> getCatalogProducts() throws Exception {
        Message response = ms.getResponse(new GetCatalogProducts());
        System.out.println(response.getClass());
        ContentMessage<ArrayList<Product>> cm = (ContentMessage<ArrayList<Product>>) response;
        return cm.getBody();
    }
}
