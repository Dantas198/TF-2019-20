package server;

import business.SuperMarketImpl;
import business.customer.Customer;
import business.customer.CustomerSQLImpl;
import business.data.customer.CustomerSQLDAO;
import business.data.order.OrderSQLDAO;
import business.data.product.ProductSQLDAO;
import business.order.Order;
import business.product.Product;
import client.message.bodies.AddProductBody;
import client.message.*;
import middleware.certifier.BitWriteSet;
import middleware.Server;
import middleware.ServerImpl;
import middleware.certifier.StateUpdates;
import middleware.certifier.StateUpdatesBitSet;
import middleware.message.ContentMessage;
import middleware.message.ErrorMessage;
import middleware.message.Message;
import middleware.message.TransactionMessage;
import middleware.message.replication.CertifyWriteMessage;
import org.apache.commons.math3.geometry.partitioning.BSPTreeVisitor;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class GandaGotaServerImpl extends ServerImpl<BitSet, BitWriteSet, ArrayList<String>> {

    private SuperMarketImpl superMarket;
    private Connection connection;

    public GandaGotaServerImpl(int spreadPort, String privateName, int atomixPort, Connection connection) throws SQLException {
        super(spreadPort, privateName, atomixPort, connection);
        //TODO tmax não poderá aumentar/diminuir consoante a quantidade de aborts
        this.superMarket = new SuperMarketImpl(privateName, connection);
        this.connection = connection;
    }

    public GandaGotaServerImpl(int spreadPort, String privateName, int atomixPort) throws SQLException {
        this(spreadPort, privateName, atomixPort, DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:9001/" + privateName, "user", "password"));
    }


    @Override
    public Message handleMessage(Message message) {
        try {
            if(message instanceof AddCustomerMessage) {
                String customer = ((AddCustomerMessage) message).getBody();
                return new ContentMessage<>(superMarket.addCustomer(customer));
            }
            if(message instanceof GetOrderMessage) {
                String customer = ((GetOrderMessage) message).getBody();
                Map<Product, Integer> currentOrderProducts = superMarket.getCurrentOrderProducts(customer);
                HashMap<Product, Integer> response = null;
                if(currentOrderProducts != null) {
                    response = new HashMap<>(currentOrderProducts);
                }
                return new ContentMessage<>(response);
            }
            if(message instanceof AddProductMessage) {
                AddProductBody body = ((AddProductMessage) message).getBody();
                String customer = body.getCustomer();
                String product = body.getProduct();
                int amount = body.getAmount();
                return new ContentMessage<>(superMarket.addProduct(customer, product, amount));
            }
            if(message instanceof ResetOrderMessage) {
                String customer = ((ResetOrderMessage) message).getBody();
                return new ContentMessage<>(superMarket.resetOrder(customer));
            }
            if(message instanceof GetCatalogProductsMessage){
                return new ContentMessage<>(new ArrayList<>(superMarket.getCatalogProducts()));
            }
            if(message instanceof GetHistoryMessage) {
                return new ContentMessage<>(new ArrayList<>(superMarket.getHistory(((GetHistoryMessage) message).getBody())));
            }
        } catch (Exception e){
            e.printStackTrace();
            return new ErrorMessage(e).from(message);
        }
        return new ErrorMessage(new Exception("Unrecognized message " + message.getId() + ": " + message));
    }

    @Override
    //TODO mudar o estado
    public CertifyWriteMessage<BitWriteSet, ?> handleTransactionMessage(TransactionMessage<?> message){
        StateUpdates<String, Serializable> updates = new StateUpdatesBitSet<>();
        if (message instanceof FinishOrderMessage) {
            String customer = ((FinishOrderMessage) message).getBody();
            boolean success = superMarket.finishOrder(customer, updates);
            if(success) System.out.println("correu bem");
        }
        return new CertifyWriteMessage<>(updates.getWriteSets(), (HashMap<String, Set<Serializable>>) updates.getAllUpdates());
    }

    @Override
    public void updateStateFromCommitedWrite(CertifyWriteMessage<BitWriteSet, ?> message) {
        //TODO
        //TESTE
        System.out.println("Server : " + this.getPrivateName() + " update state from commit");
        int state = (Integer) message.getState();
    }

    @Override
    public void commit(Object state) throws SQLException {
            //TODO
            Map<String, Set<Serializable>> changes = (Map<String, Set<Serializable>>) state;
            for (Map.Entry<String, Set<Serializable>> entry : changes.entrySet()) {
                String tag = entry.getKey();
                Set<?> objects = entry.getValue();
                switch (tag) {
                    case "customer": {
                        OrderSQLDAO orderSQLDAO = new OrderSQLDAO(this.connection);
                        CustomerSQLDAO customerSQLDAO = new CustomerSQLDAO(this.connection, orderSQLDAO);
                        for (Customer customer : (Set<Customer>) objects) {
                            customerSQLDAO.put(customer);
                        }
                    }
                    break;
                    case "order": {
                        OrderSQLDAO orderSQLDAO = new OrderSQLDAO(this.connection);
                        for (Order order : (Set<Order>) objects) {
                            orderSQLDAO.put(order);
                        }
                    }
                    break;
                    case "product": {
                        ProductSQLDAO productSQLDAO = new ProductSQLDAO(this.connection);
                        for(Product product : (Set<Product>) objects) {
                            productSQLDAO.put(product);
                    }
                    break;
                }
            }
        }
        System.out.println("Server : " + this.getPrivateName() + " commit");
    }

    @Override
    public void rollback(){
        //TODO
        System.out.println("Server : " + this.getPrivateName() + " rollback");
    }

    @Override
    public ArrayList<String> getState() {
        return null;
    }

    @Override
    public void setState(ArrayList<String> queries) {
        try {
            Connection c = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:9001/1", "user", "password");
            for(String query : queries) {
                c.prepareStatement(query).execute();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        String serverName = args.length < 1 ? "Server1" : args[0];
        int spreadPort = args.length < 2 ? 4803 : Integer.parseInt(args[1]);
        int atomixPort = args.length < 3 ? 6666 : Integer.parseInt(args[2]);
        new HSQLServer(serverName).start();
        Server server = new GandaGotaServerImpl(spreadPort, serverName, atomixPort);
        server.start();
    }
}
