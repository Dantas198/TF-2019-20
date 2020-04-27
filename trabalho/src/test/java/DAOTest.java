import business.customer.Customer;
import business.customer.CustomerImpl;
import business.data.CustomerSQLDAO;
import business.data.DAO;
import business.data.OrderDAO;
import business.order.Order;
import business.order.OrderImpl;
import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Set;

import static org.junit.Assert.*;

public class DAOTest {

    @Test
    public void OrderDAOTest(){
        DAO<String, Order> orderDAO = new OrderDAO();
        Order order1 = new OrderImpl();
        daoTest(orderDAO, order1.getId(), order1);
    }

    @Test
    public void CustomerDAOTest() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:hsqldb:testdb;shutdown=true", "", "");
        CustomerSQLDAO dao = new CustomerSQLDAO(c, true);
        Customer customer1 = new CustomerImpl("Customer1");
        daoTest(dao, customer1.getId(), customer1);
        Customer customer = dao.get(customer1.getId());
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
        for (Order o:
             list) {
            assertEquals("Iterator should contain same elements",
                    o, list.get(i));
            i++;
        }
    }

    private <K,T> void daoTest(DAO<K, T>  dao, K key, T obj){
        dao.put(obj);
        T obj2 = dao.get(key);
        assertEquals("Adding and retrieving the object with same id aren't equal (" + key + "," + obj + ")",
                obj, obj2);
        dao.delete(key);
        obj2 = dao.get(key);
        assertNull("Retrieving an object after its deletion should be null", obj2);
        int beforeLength = dao.getAll().size();
        dao.put(obj);
        int length = dao.getAll().size();
        assertEquals("Adding an existing object to dao should not increase database size", beforeLength + 1, length);
    }
}
