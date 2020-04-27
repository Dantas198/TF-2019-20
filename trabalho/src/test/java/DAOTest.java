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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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
