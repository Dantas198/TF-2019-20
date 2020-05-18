import business.customer.Customer;
import business.customer.CustomerImpl;
import business.data.DBInitialization;
import business.data.customer.CustomerDAO;
import business.data.customer.CustomerSQLDAO;
import business.data.DAO;
import business.data.order.OrderDAO;
import business.data.order.OrderSQLDAO;
import business.data.product.OrderProductDAO;
import business.data.product.ProductDAO;
import business.data.product.ProductSQLDAO;
import business.order.Order;
import business.order.OrderImpl;
import business.product.Product;
import business.product.ProductImpl;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

public class DAOTest {

    private void ProductDAOTest(DAO<String, Product> dao){
        Product product1 = new ProductImpl("Product 1", 1.0f, "abcd");
        daoTest(dao, product1.getName(), product1);
    }

    private void OrderDAOTest(DAO<String, Order> dao){
        Order order1 = new OrderImpl();
        daoTest(dao, order1.getId(), order1);
    }


    @Test
    public void SQLDAOTest() throws SQLException, ExecutionException, InterruptedException {
        CustomerDAOTest(new CustomerDAO());
        OrderDAOTest(new OrderDAO());
        ProductDAOTest(new ProductDAO());
        Connection c = DriverManager.getConnection("jdbc:hsqldb:file:testdb;shutdown=true;hsqldb.sqllog=2", "", "");
        new DBInitialization(c).init();
        CustomerDAOTest(new CustomerSQLDAO(c, new OrderSQLDAO(c)));
        OrderDAOTest(new OrderSQLDAO(c));
        System.out.println("OI");
        ProductDAOTest(new ProductSQLDAO(c));
    }


    private void CustomerDAOTest(DAO<String, Customer> dao) {
        Customer customer1 = new CustomerImpl("Customer1");
        daoTest(dao, customer1.getId(), customer1);
        String customerName = "Customer2";
        dao.put(new CustomerImpl(customerName));
        Customer customer = dao.get(customerName);
        Set<Order> oldOrders = customer.getOldOrders();
        assertTrue("Customer's old orders should initialize empty", oldOrders.isEmpty());
        assertEquals("Customer's old orders' size should be coherent with empty",
                0, oldOrders.size());
        Order order = new OrderImpl();
        oldOrders.add(order);
        assertFalse("Customer's old orders should have content", oldOrders.isEmpty());
        assertEquals("Customer's old orders' size should have one order",
                1, oldOrders.size());
        assertTrue("Customer's old orders should have the order", oldOrders.contains(order));
        ArrayList<Order> list = new ArrayList<>();
        list.add(order);
        list.add(new OrderImpl());
        list.add(new OrderImpl());
        int i = 0;
        for (Order o: oldOrders) {
            assertEquals("Iterator should contain same elements",
                    o, list.get(i));
            Product product1 = new ProductImpl("Product 1", 1.0f, "Description");
            Product product2 = new ProductImpl("Product 2", 1.0f, "Description");
            o.addProduct(product1, 1);
            o.addProduct(product2, 2);
            assertEquals("Should have 1 product1 in the order", 1, o.getProducts().get(product1).intValue());
            assertEquals("Should have 2 product2 in the order", 2, o.getProducts().get(product2).intValue());
            Set<Map.Entry<Product, Integer>> entrySet = o.getProducts().entrySet();
            assertEquals("Should have 2 products in the order", 2, entrySet.size());
            entrySet.remove(Map.entry(product1, 1));
            assertEquals("Should have 1 product in the order", 1, entrySet.size());
            entrySet.remove(Map.entry(product2, 2));
            assertTrue("Shouldn't have products in the order", entrySet.isEmpty());
            i++;
        }
        customer.getOldOrders().clear();
        assertEquals("Should have no order", 0, customer.getOldOrders().size());
        dao.delete(customerName);
        assertEquals("Should have no customer", 0, dao.getAll().size());
    }

    private <K,T> void daoTest(DAO<K, T>  dao, K key, T obj){
        dao.put(obj);
        T obj2 = dao.get(key);
        assertEquals("Adding and retrieving the object with same id aren't equal (" + key + "," + obj + ")",
                obj, obj2);
        int beforeLength = dao.getAll().size();
        dao.put(obj);
        int length = dao.getAll().size();
        assertEquals("Adding an existing object to dao should not increase database size", beforeLength, length);
        dao.delete(key);
        obj2 = dao.get(key);
        assertNull("Retrieving an object after its deletion should be null", obj2);
    }
}
