package business.data.order;

import business.data.DAOPS;
import business.data.SQLDAO;
import business.data.product.OrderProductDAO;
import business.order.Order;
import business.order.OrderImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class OrderSQLDAO extends SQLDAO<String, Order> {
    public OrderSQLDAO(Connection c) throws SQLException {
        super(c, new DAOPS<>() {
            PreparedStatement getPS = c.prepareStatement("SELECT * FROM \"order\" WHERE \"id\" = ?");
            PreparedStatement putPS = c.prepareStatement("REPLACE INTO \"order\" (\"id\") VALUES (?)");
            PreparedStatement deletePS = c.prepareStatement("DELETE FROM \"order\" WHERE \"id\" = ?");
            PreparedStatement updatePS = c.prepareStatement("UPDATE \"order\" SET \"id\" = ?, \"customer_id\" = ? WHERE \"id\" = ?");
            PreparedStatement getAllPS = c.prepareStatement("SELECT * FROM \"order\"");

            @Override
            public Order fromResultSet(ResultSet resultSet) throws SQLException {
                String id = resultSet.getString("id");
                Date timestamp = resultSet.getTimestamp("timestamp");
                return new OrderImpl(id, new OrderProductDAO(c, id), timestamp);
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
                updatePS.setString(2, key);
                return updatePS;
            }

            @Override
            public PreparedStatement getAll() {
                return getAllPS;
            }
        });
    }
}
