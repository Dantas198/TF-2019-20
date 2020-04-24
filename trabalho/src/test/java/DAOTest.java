import business.data.DAO;
import business.data.OrderDAO;
import business.order.Order;
import business.order.OrderImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DAOTest {

    @Test
    public void OrderDAOTest(){
        DAO<String, Order> orderDAO = new OrderDAO();
        Order order1 = new OrderImpl();
        orderDAO.put(order1);
        Order order2 = orderDAO.get(order1.getId());
        assertEquals( "Adding and retrieving the order with same id aren't equal", order1, order2);
    }
}
