package business.data.customer;

import business.customer.Customer;
import business.customer.CustomerImpl;
import business.customer.CustomerSQLImpl;
import business.data.DAOPS;
import business.data.SQLDAO;
import business.data.order.OrderDAO;
import business.data.order.OrderSQLDAO;
import business.order.Order;
import business.order.OrderImpl;

import java.sql.*;

public class CustomerSQLDAO extends SQLDAO<String, Customer> {
    public CustomerSQLDAO(Connection c, OrderSQLDAO orderDAO) throws SQLException {
        super(c, new DAOPS<>() {
            PreparedStatement getPS = c.prepareStatement("SELECT * FROM \"customer\" WHERE \"id\" = ?");
            PreparedStatement putPS = c.prepareStatement("INSERT INTO \"customer\" (\"id\", \"current_order_id\") VALUES (?, ?) ON DUPLICATE KEY UPDATE \"current_order_id\" = ?");
            PreparedStatement deletePS = c.prepareStatement("DELETE FROM \"customer\" WHERE \"id\" = ?");
            PreparedStatement updatePS = c.prepareStatement("UPDATE \"customer\" SET \"id\" = ?, \"current_order_id\" = ? WHERE \"id\" = ?");
            PreparedStatement getAllPS = c.prepareStatement("SELECT * FROM \"customer\"");

            @Override
            public Customer fromResultSet(ResultSet resultSet) throws SQLException {
                String id = resultSet.getString("id");
                String currentOrder = resultSet.getString("current_order_id");
                Order order = currentOrder != null ? new OrderImpl(currentOrder) : null;
                return new CustomerSQLImpl(id,
                        order,
                        new CustomerOldOrderDAO(c, id, currentOrder),
                        orderDAO,
                        c
                );
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
            public PreparedStatement put(Customer o) throws SQLException {
                putPS.setString(1, o.getId());
                String currentOrder = o.hasCurrentOrder() ?
                    o.getCurrentOrder().getId() :
                    null;
                putPS.setString(2, currentOrder);
                putPS.setString(3, currentOrder);
                return putPS;
            }

            @Override
            public PreparedStatement delete(String key) throws SQLException {
                deletePS.setString(1, key);
                return deletePS;
            }

            @Override
            public PreparedStatement update(String key, Customer o) throws SQLException {
                updatePS.setString(1, o.getId());
                String orderId;
                if(o.hasCurrentOrder()) {
                    orderId = o.getCurrentOrder().getId();
                } else {
                    orderId = null;
                }
                updatePS.setString(2, orderId);
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