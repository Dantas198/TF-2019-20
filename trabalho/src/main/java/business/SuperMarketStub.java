package business;

import business.order.Order;
import business.product.Product;
import client.message.AddCustumer;
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
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class SuperMarketStub implements SuperMarket {
    private ManagedMessagingService mms;
    private Address primaryServer;
    private Address myAddress;
    private Serializer s;
    private int idCount;
    private CompletableFuture<Message> res;
    private ScheduledExecutorService ses;
    private Order currentOrder;

    public SuperMarketStub(int myPort, Address primaryServer){
        this.res = null;
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
                res.complete(repm);
        }, ses);
    }

    public ScheduledFuture<?> scheduleTimeout(Message reqm){
        return ses.scheduleAtFixedRate(()->{
            System.out.println("timeout...sending new request");
            mms.sendAsync(primaryServer, "request", s.encode(reqm));
        }, 1, 4, TimeUnit.SECONDS);
    }

    @Override
    public boolean addCustomer(String customer) {
        Message reqm = new AddCustumer(customer);
        res = new CompletableFuture<>();
        ScheduledFuture<?> sf = scheduleTimeout(reqm);
        res.whenComplete((m,t) -> sf.cancel(true));
        return true;
    }


    @Override
    public boolean resetOrder(String customer) {
        return false;
    }

    @Override
    public boolean finishOrder(String customer) {
        return false;
    }

    @Override
    public boolean addProduct(String customer, String name, int amount) {
        return false;
    }

    @Override
    public Map<Product, Integer> getCurrentOrderProducts(String customer) {
        return null;
    }

    @Override
    public List<Order> getHistory(String customer) {
        return null;
    }

    @Override
    public Collection<Product> getCatalogProducts() {
        return null;
    }
}
