package business;

import business.SuperMarket;

import business.customer.Customer;
import business.order.Order;
import business.product.Product;
import client.message.GetProductsMessage;
import io.atomix.cluster.messaging.ManagedMessagingService;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.atomix.utils.serializer.Serializer;
import io.atomix.utils.serializer.SerializerBuilder;
import middleware.message.Message;


import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;

public class SuperMarketStub implements SuperMarket {
    private ManagedMessagingService mms;
    private Address primaryServer;
    private Address myAddress;
    private Serializer s;
    private int idCount;
    private Map<String, CompletableFuture<Message>> res;
    private ScheduledExecutorService ses;

    public SuperMarketStub(int myPort, Address primaryServer){
        this.res = new HashMap<>();
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
                .addType(Message.class)
                .build();

        this.mms.registerHandler("reply", (a,b) -> {
            Message repm = s.decode(b);
            if(res.containsKey(repm.getId())){
                res.get(repm.getId()).complete(repm);
            }
        }, ses);
    }


    @Override
    public boolean addCustomer(Customer customer) {
        return false;
    }

    @Override
    public void addToOrder(Customer client, Product prod, int quantity) {

    }

    @Override
    public void buyOrder(Customer client) {

    }

    @Override
    public Order getOrder(Customer client) {
        return null;
    }

    @Override
    public Product getProduct(String name) {
        return null;
    }

    @Override
    public Collection<Product> getProducts() throws Exception {
        Message reqm = new GetProductsMessage();
        CompletableFuture<Message> res = new CompletableFuture<>();
        ScheduledFuture<?> sf = scheduleTimeout(reqm);
        //Caso a resposta tenha chegado cancela o timeout
        res.whenComplete((m,t) -> sf.cancel(true));
        mms.sendAsync(primaryServer, "request", s.encode(reqm));
        Serializable body = res.get();
        if(body instanceof Collection){
            return (Collection<Product>) body;
        }
        else return null;
    }


    public ScheduledFuture<?> scheduleTimeout(Message reqm){
        return ses.scheduleAtFixedRate(()->{
            System.out.println("timeout...sending new request");
            mms.sendAsync(primaryServer, "request", s.encode(reqm));
        }, 1, 4, TimeUnit.SECONDS);
    }
}
