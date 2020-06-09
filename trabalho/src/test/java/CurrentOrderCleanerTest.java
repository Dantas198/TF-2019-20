import business.customer.Customer;
import business.customer.CustomerImpl;
import business.data.DBInitialization;
import business.data.customer.CustomerDAO;
import business.data.customer.CustomerSQLDAO;
import business.data.order.OrderSQLDAO;
import org.junit.Test;
import server.CurrentOrderCleaner;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Calendar;

import static org.junit.Assert.*;

public class CurrentOrderCleanerTest {
    @Test
    public void test() throws SQLException {
        Connection c = DriverManager.getConnection("jdbc:hsqldb:file:db/currentOrderCleanerTest2;shutdown=true;hsqldb.sqllog=2;hsqldb.lock_file=false;sql.syntax_mys=true", "", "");
        new DBInitialization(c).init();
        CustomerSQLDAO customerSQLDAO = new CustomerSQLDAO(c);
        Calendar calendar = Calendar.getInstance();
        CurrentOrderCleaner cleaner = new CurrentOrderCleaner(customerSQLDAO, Duration.ofDays(10), calendar);
        customerSQLDAO.put(new CustomerImpl("Customer 1"));
        customerSQLDAO.get("Customer 1").newCurrentOrder();
        assertNull("Next clean should be null", cleaner.getNextClean());
        assertNotNull("Customer should have current order", customerSQLDAO.get("Customer 1").getCurrentOrder());
        cleaner.clean();
        assertNotNull("Next clean should be not null", cleaner.getNextClean());
        assertNotNull("Customer should have current order", customerSQLDAO.get("Customer 1").getCurrentOrder());
        calendar.add(Calendar.MONTH, 1);
        System.out.println(calendar.getTime());
        System.out.println(">" + customerSQLDAO.get("Customer 1").getCurrentOrder());
        cleaner.clean();
        System.out.println(customerSQLDAO.get("Customer 1").getCurrentOrder());
        Customer customer = customerSQLDAO.get("Customer 1");
        assertNull("Customer shouldn't have current order", customerSQLDAO.get("Customer 1").getCurrentOrder());
    }
}
