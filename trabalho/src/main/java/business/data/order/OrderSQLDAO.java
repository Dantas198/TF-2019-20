package business.data.order;

import business.data.DAOMap;
import business.data.DAOPS;
import business.data.SQLDAO;
import business.order.Order;
import business.order.OrderImpl;
import business.product.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.function.Function;

public class OrderSQLDAO extends SQLDAO<String, Order> {
    public OrderSQLDAO(Connection c, Function<String, DAOMap<Product, Integer>> orderProductDAOfunc) throws SQLException {
        super(c, new DAOPS<>() {
            PreparedStatement getPS = c.prepareStatement("SELECT * FROM \"order\" WHERE \"id\" = ?");
            PreparedStatement putPS = c.prepareStatement("REPLACE INTO \"order\" (\"id\", \"customer_id\") VALUES (?, ?)");
            PreparedStatement deletePS = c.prepareStatement("DELETE FROM \"order\" WHERE \"id\" = ?");
            PreparedStatement updatePS = c.prepareStatement("UPDATE \"order\" SET \"id\" = ?, \"customer_id\" = ? WHERE \"id\" = ?");
            PreparedStatement getAllPS = c.prepareStatement("SELECT * FROM \"order\"");

            @Override
            public Order fromResultSet(ResultSet resultSet) throws SQLException {
                String id = resultSet.getString("id");
                Date timestamp = resultSet.getTimestamp("timestamp");
                String customerId = resultSet.getString("customer_id");
                return new OrderImpl(id, orderProductDAOfunc.apply(id), timestamp, customerId);
            }

            @Override
            public String getKey(ResultSet resultSet) throws SQLException {
                return resultSet.getString("id");
            }

            @Override
            public PreparedStatement get(String key) throws SQLException {
                getPS.setString(1, key);
                return getPS;
            }

            @Override
            public PreparedStatement put(Order o) throws SQLException {
                putPS.setString(1, o.getId());
                putPS.setString(2, o.getCustomerId());
                return putPS;
            }

            @Override
            public PreparedStatement delete(String key) throws SQLException {
                deletePS.setString(1, key);
                return deletePS;
            }

            @Override
            public PreparedStatement update(String key, Order o) throws SQLException {
                updatePS.setString(1, o.getId());
                updatePS.setString(2, o.getCustomerId());
                updatePS.setString(3, key);
                return updatePS;
            }

            @Override
            public PreparedStatement getAll() {
                return getAllPS;
            }
        });
    }
}
