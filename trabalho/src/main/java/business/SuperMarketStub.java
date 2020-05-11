package business;

import business.order.Order;
import business.order.OrderImpl;
import business.product.Product;
import business.product.ProductImpl;
import client.message.AddCostumerMessage;
import client.message.FinishOrderMessage;
import client.message.GetCatalogProducts;
import client.message.GetHistoryMessage;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import middleware.message.ContentMessage;
import middleware.message.Message;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.*;

public class SuperMarketStub implements SuperMarket {
    private ManagedMessagingService mms;
    private Address primaryServer;
    private Serializer s;
    private CompletableFuture<Message> res;
    private ScheduledExecutorService ses;
    private String privateCustumer;
    private Order currentOrder;

    public SuperMarketStub(int myPort, Address primaryServer){
        this.res = null;
        this.currentOrder = null;
        this.primaryServer = primaryServer;
        Address myAddress = io.atomix.utils.net.Address.from("localhost", myPort);
        this.ses = Executors.newScheduledThreadPool(1);

        this.mms = new NettyMessagingService(
                "server",
                myAddress,
                new MessagingConfig());
        this.mms.start();
        this.s = new SerializerBuilder()
                .withRegistrationRequired(false)
                .build();

        this.mms.registerHandler("reply", (a,b) -> {
            try{
                System.out.println("HELLO1");
                Message repm = s.decode(b);
                System.out.println("HELLO2");
                res.complete(repm);
                System.out.println("HELLO3");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ses);
    }

    public Message getResponse(Message reqm) throws Exception{
        res = new CompletableFuture<>();
        ScheduledFuture<?> sf = scheduleTimeout(reqm);
        //Caso a resposta tenha chegado cancela o timeout
        res.whenComplete((m,t) -> { t.printStackTrace(); sf.cancel(true);});
        mms.sendAsync(primaryServer, "request", s.encode(reqm));
        return res.thenApply(cm -> {System.out.println("CLIENT: Received -> " + cm.getId()); return cm;}).get();
    }


    public ScheduledFuture<?> scheduleTimeout(Message reqm){
        return ses.scheduleAtFixedRate(()->{
            System.out.println("timeout...sending new request");
            mms.sendAsync(primaryServer, "request", s.encode(reqm));
        }, 1, 4, TimeUnit.SECONDS);
    }

    @Override
    public boolean addCustomer(String customer) throws Exception {
        this.privateCustumer = customer;
        return (Boolean) ((ContentMessage) (new AddCostumerMessage(customer))).getBody();
    }

    @Override
    public boolean resetOrder(String customer) {
        this.currentOrder.reset();
        return true;
    }

    @Override
    public boolean finishOrder(String customer) throws Exception {
        if(!customer.equals(privateCustumer))
            return false;
        return (Boolean) ((ContentMessage) getResponse(new FinishOrderMessage(customer, currentOrder))).getBody();
    }

    @Override
    public boolean addProduct(String customer, String product, int amount) {
        if(!customer.equals(privateCustumer))
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
        ContentMessage<ArrayList<Order>> cm = ((ContentMessage)  getResponse(new GetHistoryMessage(customer)));
        return cm.getBody();
    }

    @Override
    public ArrayList<Product> getCatalogProducts() throws Exception {
        Message response = getResponse(new GetCatalogProducts());
        System.out.println(response.getClass());
        ContentMessage<ArrayList<Product>> cm = (ContentMessage<ArrayList<Product>>) response;
        return cm.getBody();
    }
}
