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
    public CustomerOldOrderDAO(Connection c, String current_id, String current_order_id) throws SQLException {
        super(new DAOSetPS<>() {
            @Override
            public Order fromResultSet(ResultSet resultSet) throws SQLException {
                String id = resultSet.getString("id");
                return new OrderImpl(id, new OrderProductDAO(c, id));
            }

            @Override
            public PreparedStatement getAll() throws SQLException {
                String current_order_restrinction = current_order_id == null ? "" : " AND NOT \"id\" <> ?";
                PreparedStatement ps = c.prepareStatement("SELECT * FROM \"order\" WHERE \"customer_id\" = ? " + current_order_restrinction);
                ps.setString(1, current_id);
                if(current_order_id != null)
                    ps.setString(2, current_order_id);
                return ps;
            }

            @Override
            public PreparedStatement size() throws SQLException {
                String current_order_restrinction = current_order_id == null ? "" : " AND NOT \"id\" <> ?";
                PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM \"order\" WHERE \"customer_id\" = ?" + current_order_restrinction);
                ps.setString(1, current_id);
                if(current_order_id != null)
                    ps.setString(2, current_order_id);
                return ps;
            }

            @Override
            public PreparedStatement add(Order o) throws SQLException {
                // TODO: Não inserir se já for a order atual
                PreparedStatement ps = c.prepareStatement("REPLACE INTO \"order\" (\"id\", \"customer_id\") VALUES (?, ?)");
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
                PreparedStatement ps = c.prepareStatement("DELETE FROM \"order\" WHERE \"customer_id\" = ?");
                ps.setString(1, current_id);
                return ps;
            }

            @Override
            public PreparedStatement empty() {
                return null;
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
