package server;

import business.SuperMarketImpl;
import business.customer.Customer;
import business.data.DAO;
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
import middleware.message.WriteMessage;
import middleware.message.replication.CertifyWriteMessage;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;

public class GandaGotaServerImpl extends ServerImpl<BitSet, BitWriteSet, ArrayList<String>> {

    private SuperMarketImpl superMarket;
    private Connection connection;
    private DAO<String, Order> orderDAO;
    private DAO<String, Product> productDAO;
    private DAO<String, Customer> customerDAO;

    public GandaGotaServerImpl(int spreadPort, String privateName, int atomixPort, Connection connection) throws SQLException {
        super(spreadPort, privateName, atomixPort, connection);
        //TODO tmax não poderá aumentar/diminuir consoante a quantidade de aborts
        this.connection = connection;
        this.orderDAO = new OrderSQLDAO(this.connection);
        this.productDAO = new ProductSQLDAO(this.connection);
        this.customerDAO = new CustomerSQLDAO(this.connection);
        this.superMarket = new SuperMarketImpl(orderDAO, productDAO, customerDAO);
    }

    public GandaGotaServerImpl(int spreadPort, String privateName, int atomixPort) throws SQLException {
        this(spreadPort, privateName, atomixPort, DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:9001/" + privateName, "user", "password"));
    }


    @Override
    public Message handleMessage(Message message) {
        try {
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
    public CertifyWriteMessage<BitWriteSet, ?> handleWriteMessage(WriteMessage<?> message){
        StateUpdates<String, Serializable> updates = new StateUpdatesBitSet<>();
        boolean success = false;
        if(message instanceof AddCustomerMessage) {
            String customer = ((AddCustomerMessage) message).getBody();
            success = superMarket.addCustomer(customer, updates);
        } else if (message instanceof FinishOrderMessage) {
            String customer = ((FinishOrderMessage) message).getBody();
            success = superMarket.finishOrder(customer, updates);
        }
        if(success) System.out.println("correu bem");
        return new CertifyWriteMessage<>(updates.getWriteSets(), (HashMap<String, Set<Serializable>>) updates.getAllUpdates());
    }

    @Override
    public void updateStateFromCommitedWrite(CertifyWriteMessage<BitWriteSet, ?> message) {
        //TODO
        //TESTE
        System.out.println("Server : " + this.getPrivateName() + " update state from commit");
        try {
            commit(message.getState());
        } catch (Exception e) {
            //TODO: Se algo corre mal, o servidor tem de parar para ficar consistente??
            System.exit(1);
        }
    }

    @Override
    public void commit(Object state) throws SQLException {
        // this.connection.setAutoCommit(false);
        //TODO verificar se é necessário transação
        Map<String, Set<Serializable>> changes = (Map<String, Set<Serializable>>) state;
        for (Map.Entry<String, Set<Serializable>> entry : changes.entrySet()) {
            String tag = entry.getKey();
            Set<?> objects = entry.getValue();
            switch (tag) {
                case "customer": {
                    for (Customer customer : (Set<Customer>) objects) {
                        customerDAO.put(customer);
                    }
                }
                break;
                case "order": {
                    for (Order order : (Set<Order>) objects) {
                        orderDAO.put(order);
                    }
                }
                break;
                case "product": {
                    for (Product product : (Set<Product>) objects) {
                        productDAO.put(product);
                    }
                    break;
                }
            }
        }
        //this.connection.commit();
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
