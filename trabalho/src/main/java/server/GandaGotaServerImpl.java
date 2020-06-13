package server;

import business.SuperMarket;
import business.SuperMarketImpl;
import business.customer.Customer;
import business.data.DAO;
import business.data.customer.CustomerCertifierDAO;
import business.data.customer.CustomerSQLDAO;
import business.data.order.OrderCertifierDAO;
import business.data.order.OrderProductDAO;
import business.data.order.OrderSQLDAO;
import business.data.product.ProductCertifierDAO;
import business.data.product.ProductSQLDAO;
import business.order.Order;
import business.order.OrderImpl;
import business.product.OrderProductQuantity;
import business.product.Product;
import business.product.ProductPlaceholder;
import client.message.bodies.AddProductBody;
import client.message.*;
import client.message.bodies.UpdateProductBody;
import middleware.GlobalEvent;
import middleware.certifier.*;
import middleware.ServerImpl;
import middleware.dbutils.ConnectionReaderManager;
import middleware.message.ContentMessage;
import middleware.message.ErrorMessage;
import middleware.message.Message;
import middleware.message.WriteMessage;
import middleware.message.replication.CertifyWriteMessage;
import middleware.message.replication.GlobalEventMessage;


import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class GandaGotaServerImpl extends ServerImpl<ArrayList<String>> {

    private Connection connection;
    private Duration tmax = Duration.ofSeconds(30);
    private CurrentOrderCleaner currentOrderCleaner;
    private ConnectionReaderManager<Connection> connectionManager;
    private int maxConnection = 20;

    public GandaGotaServerImpl(int spreadPort,
                               String privateName,
                               int atomixPort,
                               String dbStrConnection,
                               int totalServerCount,
                               String logPath) throws Exception {
        super(spreadPort, privateName, atomixPort, dbStrConnection, totalServerCount, logPath, new ArrayList<>(Collections.singletonList(new GlobalEvent("", 1))));
        this.connectionManager = new ConnectionReaderManagerImpl(maxConnection, dbStrConnection);
        this.connection = DriverManager.getConnection(dbStrConnection);
        this.currentOrderCleaner = new CurrentOrderCleaner(connection, tmax);
    }

    @Override
    public Message handleMessage(Message message) {
        try {
            Connection connection = connectionManager.assignRequest().get();
            DAO<String, Order> orderDAO = new OrderSQLDAO(connection, id -> {
                try {
                    return new OrderProductDAO(connection, id);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                return null;
            });
            DAO<String, Product> productDAO = new ProductSQLDAO(connection);
            DAO<String, Customer> customerDAO = new CustomerSQLDAO(connection);
            SuperMarket superMarket = new SuperMarketImpl(orderDAO, productDAO, customerDAO, null, tmax);
            if(message instanceof GetOrderMessage) {
                String customer = ((GetOrderMessage) message).getBody();
                Map<Product, Integer> result = superMarket.getCurrentOrderProducts(customer);
                HashMap<Product, Integer> response = result != null ? new HashMap<>(result) : null;
                return new ContentMessage<>(response);
            }
            if(message instanceof GetCatalogProductsMessage){
                return new ContentMessage<>(new ArrayList<>(superMarket.getCatalogProducts()));
            }
            if(message instanceof GetHistoryMessage) {
                ArrayList<Order> response = new ArrayList<>(0);
                for (Order order:
                        superMarket.getHistory(((GetHistoryMessage) message).getBody())) {
                    response.add(new OrderImpl(order.getId(),
                            new HashMap<>(order.getProducts()),
                            order.getTimestamp(),
                            order.getCustomerId()));
                }
                return new ContentMessage<>(response);
            }
        } catch (Exception e){
            e.printStackTrace();
            return new ErrorMessage(e).from(message);
        }
        return new ErrorMessage(new Exception("Unrecognized message " + message.getId() + ": " + message));
    }


    /**
     * returns null if execution fails
     * @return true is there is update to do in the database
     */
    @Override
    public boolean handleWriteMessage(WriteMessage<?> message, StateUpdates<String, Serializable> updates){
        SuperMarket superMarket;
        try {
            Connection connection = connectionManager.assignRequest().get();
            DAO<String, Order> orderDAO = new OrderSQLDAO(connection, id -> {
                try {
                    return new OrderProductDAO(connection, id);
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
                return null;
            });
            DAO<String, Product> productDAO = new ProductSQLDAO(connection);
            DAO<String, Customer> customerDAO = new CustomerSQLDAO(connection);
            superMarket = new SuperMarketImpl(new OrderCertifierDAO(orderDAO, updates), new ProductCertifierDAO(productDAO, updates), new CustomerCertifierDAO(customerDAO, updates), updates, tmax);
        } catch (Exception e) {
            return false;
        }
        boolean success = false;

        if(message instanceof AddCustomerMessage) {
            String customer = ((AddCustomerMessage) message).getBody();
            success = superMarket.addCustomer(customer);

        } else if (message instanceof FinishOrderMessage) {
            String customer = ((FinishOrderMessage) message).getBody();
            success = superMarket.finishOrder(customer);

        } else if (message instanceof ResetOrderMessage) {
            String customer = ((ResetOrderMessage) message).getBody();
            success = superMarket.resetOrder(customer);

        } else if (message instanceof AddProductMessage) {
            AddProductBody body = ((AddProductMessage) message).getBody();
            success = superMarket.addProductToOrder(body.getCustomer(), body.getProduct(), body.getAmount());

        } else if (message instanceof UpdateProductMessage) {
            UpdateProductBody body = ((UpdateProductMessage) message).getBody();
            success = superMarket.updateProduct(body.getName(), body.getPrice(), body.getDescription(), body.getStock());
        }

        return success;
    }

    @Override
    public void commit(Set<TaggedObject<String, Serializable>> changes, Connection databaseConnection) throws SQLException {
        DAO<String, Order> orderDAO = new OrderSQLDAO(databaseConnection, id -> {
            try {
                return new OrderProductDAO(databaseConnection, id);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
            return null;
        });
        DAO<String, Product> productDAO = new ProductSQLDAO(databaseConnection);
        DAO<String, Customer> customerDAO = new CustomerSQLDAO(databaseConnection);
        for (TaggedObject<String, Serializable> change : changes) {
            String tag = change.getTag();
            String key = change.getKey();
            Serializable object = change.getObject();
            switch (tag) {
                case "customer": {
                    if(object != null) {
                        if(!customerDAO.put((Customer) object)) throw new SQLException();
                    } else {
                        if(!customerDAO.delete(key)) throw new SQLException();
                    }
                }
                break;
                case "order": {
                    if(object != null) {
                        if(!orderDAO.put((Order) object)) throw new SQLException();
                    } else {
                        if(!orderDAO.delete(key)) throw new SQLException();
                    }
                }
                break;
                case "product": {
                    if(object != null) {
                        if(!productDAO.put((Product) object)) throw new SQLException();
                    } else {
                        if(!orderDAO.delete(key)) throw new SQLException();
                    }
                }
                break;
                case "order_product": {
                    OrderProductQuantity opq = (OrderProductQuantity) object;
                    Product product = new ProductPlaceholder(opq.getProductId());
                    orderDAO.get(opq.getOrderId())
                            .getProducts()
                            .merge(product, opq.getQuantity(), Integer::sum);
                }
                break;
            }
        }
        System.out.println("Server : " + this.getPrivateName() + " commit");
    }

    @Override
    public void rollback(CertifyWriteMessage<?> message){
        System.out.println("Server : " + this.getPrivateName() + " rollback from message: " + message.getId());
    }

    @Override
    public GlobalEventMessage createEvent(GlobalEvent e) {
        return new CurrentOrderCleanerEventMessage(e, Calendar.getInstance().getTimeInMillis());
    }

    @Override
    public void handleGlobalEvent(GlobalEventMessage e) {
        try {
            System.out.println("Limpeza");
            long time = ((CurrentOrderCleanerEventMessage) e).getTime();
            currentOrderCleaner.clean(time);
        } catch (SQLException ex) {
            this.stop();
        }
    }

}