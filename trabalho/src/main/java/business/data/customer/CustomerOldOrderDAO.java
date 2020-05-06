package business.data.customer;

import business.data.DAOSet;
import business.data.DAOSetPS;
import business.data.product.OrderProductDAO;
import business.order.Order;
import business.order.OrderImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerOldOrderDAO extends DAOSet<Order> {
    public CustomerOldOrderDAO(Connection c, String current_id) throws SQLException {
        super(new DAOSetPS<Order>() {
            @Override
            public Order fromResultSet(ResultSet resultSet) throws SQLException {
                String id = resultSet.getString("id");
                return new OrderImpl(id, new OrderProductDAO(c, id));
            }

            @Override
            public PreparedStatement getAll() throws SQLException {
                PreparedStatement ps = c.prepareStatement("SELECT * FROM \"order\" WHERE \"customer_id\" = ?");
                ps.setString(1, current_id);
                return ps;
            }

            @Override
            public PreparedStatement size() throws SQLException {
                PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM \"order\" WHERE \"customer_id\" = ?");
                ps.setString(1, current_id);
                return ps;
            }

            @Override
            public PreparedStatement add(Order o) throws SQLException {
                PreparedStatement ps = c.prepareStatement("INSERT INTO \"order\" (\"id\", \"customer_id\") VALUES (?, ?)");
                ps.setString(1, o.getId());
                ps.setString(2, current_id);
                return ps;
            }

            @Override
            public PreparedStatement remove(Object o) throws SQLException {
                if(o instanceof Order) {
                    Order order = (Order) o;
                    PreparedStatement ps = c.prepareStatement("DELETE FROM \"order\" WHERE \"id\" = ?");
                    ps.setString(1, order.getId());
                    return ps;
                } else {
                    return c.prepareStatement("");
                }
            }

            @Override
            public PreparedStatement clear() throws SQLException {
                return c.prepareStatement("DELETE FROM \"order\" WHERE \"customer_id\" = ?");
            }

            @Override
            public PreparedStatement empty() throws SQLException {
                PreparedStatement ps = c.prepareStatement("SELECT 1 FROM \"order\" WHERE \"customer_id\" = ?");
                ps.setString(1, current_id);
                return ps;
            }

            @Override
            public PreparedStatement contains(Object o) throws SQLException {
                if(o instanceof Order) {
                    PreparedStatement ps = c.prepareStatement("SELECT 1 FROM \"order\" WHERE \"id\" = ?");
                    ps.setString(1, ((Order) o).getId());
                    return ps;
                } else {
                    return c.prepareStatement("");
                }
            }
        });
    }
}
