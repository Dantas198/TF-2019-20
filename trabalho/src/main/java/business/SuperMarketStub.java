package business;

import business.order.Order;
import business.product.Product;
import client.bodies.FinishOrderBody;
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
    private Address myAddress;
    private Serializer s;
    private int idCount;
    private CompletableFuture<ContentMessage> res;
    private ScheduledExecutorService ses;
    private String privateCustumer;
    private HashMap<String, Order> currentOrders;

    public SuperMarketStub(int myPort, Address primaryServer){
        this.res = null;
        this.currentOrders = new HashMap<>();
        this.primaryServer = primaryServer;
        this.idCount = 0;
        this.myAddress = io.atomix.utils.net.Address.from(myPort);
        this.ses = Executors.newScheduledThreadPool(1);

        this.mms = new NettyMessagingService(
                "server",
                myAddress,
                new MessagingConfig());
        this.mms.start();
        this.s = new SerializerBuilder()
                .addType(ContentMessage.class)
                .build();

        this.mms.registerHandler("reply", (a,b) -> {
            ContentMessage<?> repm = s.decode(b);
                res.complete(repm);
        }, ses);
    }

    public <T extends Serializable> ContentMessage<T> getResponse(Message reqm) throws Exception{
        res = new CompletableFuture<>();
        ScheduledFuture<?> sf = scheduleTimeout(reqm);
        //Caso a resposta tenha chegado cancela o timeout
        res.whenComplete((m,t) -> sf.cancel(true));
        mms.sendAsync(primaryServer, "request", s.encode(reqm));
        return res.get();
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
        return (Boolean) getResponse(new AddCostumerMessage(customer)).getBody();
    }

    @Override
    public boolean resetOrder(String customer) {
        if(!currentOrders.containsKey(customer) || !customer.equals(privateCustumer))
            return false;
        this.currentOrders.get(customer).reset();
        return true;
    }

    @Override
    public boolean finishOrder(String customer) throws Exception {
        return (Boolean) getResponse(new FinishOrderMessage(customer, currentOrders.get(customer))).getBody();
    }

    @Override
    public boolean addProduct(String customer, Product product, int amount) {
        if(!currentOrders.containsKey(customer) || !customer.equals(privateCustumer))
            return false;
        this.currentOrders.get(customer).addProduct(product, amount);
        return true;
    }

    @Override
    public Map<Product, Integer> getCurrentOrderProducts(String customer) {
        return this.currentOrders.get(customer).getProducts();
    }

    @Override
    public ArrayList<Order> getHistory(String customer) throws Exception {
        ContentMessage<ArrayList<Order>> cm = getResponse(new GetHistoryMessage(customer));
        return cm.getBody();
    }

    @Override
    public ArrayList<Product> getCatalogProducts() throws Exception {
        ContentMessage<ArrayList<Product>> cm = getResponse(new GetCatalogProducts());
        return cm.getBody();
    }
}
