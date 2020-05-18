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
    public CustomerOldOrderDAO(Connection c, String customer_id) throws SQLException {
        super(new DAOSetPS<>() {
            @Override
            public Order fromResultSet(ResultSet resultSet) throws SQLException {
                String id = resultSet.getString("id");
                return new OrderImpl(id, new OrderProductDAO(c, id));
            }

            @Override
            public PreparedStatement getAll() throws SQLException {
                PreparedStatement ps = c.prepareStatement("SELECT * FROM \"order\" INNER JOIN \"customer\" ON \"order\".\"customer_id\" = \"customer\".\"id\" WHERE \"order\".\"customer_id\" = ? AND \"order\".\"id\" <> \"customer\".\"current_order_id\"");
                ps.setString(1, customer_id);
                return ps;
            }

            @Override
            public PreparedStatement size() throws SQLException {
                PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM \"order\" INNER JOIN \"customer\" ON \"order\".\"customer_id\" = \"customer\".\"id\" WHERE \"order\".\"customer_id\" = ? AND \"order\".\"id\" <> \"customer\".\"current_order_id\"");
                ps.setString(1, customer_id);
                return ps;
            }

            @Override
            public PreparedStatement add(Order o) throws SQLException {
                PreparedStatement ps = c.prepareStatement("INSERT INTO \"order\" (\"id\", \"customer_id\") VALUES (?, ?)");
                ps.setString(1, o.getId());
                ps.setString(2, customer_id);
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
                PreparedStatement ps = c.prepareStatement("DELETE \"order\" FROM \"order\" INNER JOIN \"customer\" ON \"order\".\"customer_id\" = \"customer\".\"id\" WHERE \"order\".\"customer_id\" = ? AND \"order\".\"id\" <> \"customer\".\"current_order_id\"");
                ps.setString(1, customer_id);
                return ps;
            }

            @Override
            public PreparedStatement empty() throws SQLException {
                PreparedStatement ps = c.prepareStatement("SELECT 1 FROM \"order\" WHERE \"customer_id\" = ?");
                ps.setString(1, customer_id);
                return ps;
            }

            @Override
            public PreparedStatement contains(Object o) throws SQLException {
                if(o instanceof Order) {
                    PreparedStatement ps = c.prepareStatement("SELECT 1 FROM \"order\" INNER JOIN \"customer\" ON \"order\".\"customer_id\" = \"customer\".\"id\" WHERE \"order\".\"customer_id\" = ? AND \"order\".\"id\" <> \"customer\".\"current_order_id\"");
                    ps.setString(1, ((Order) o).getId());
                    return ps;
                } else {
                    return c.prepareStatement("");
                }
            }
        });
    }
}
