package business.data.customer;

import business.customer.Customer;
import business.customer.CustomerImpl;
import business.customer.CustomerSQLImpl;
import business.data.DAOPS;
import business.data.SQLDAO;
import business.data.order.OrderDAO;
import business.data.order.OrderSQLDAO;
import business.order.OrderImpl;

import java.sql.*;

public class CustomerSQLDAO extends SQLDAO<String, Customer> {
    public CustomerSQLDAO(Connection c, OrderSQLDAO orderDAO) throws SQLException {
        super(c, new DAOPS<>() {
            PreparedStatement getPS = c.prepareStatement("SELECT * FROM \"customer\" WHERE \"id\" = ?");
            PreparedStatement putPS = c.prepareStatement("INSERT INTO \"customer\" (\"id\", \"current_order_id\") VALUES (?, ?)");
            PreparedStatement deletePS = c.prepareStatement("DELETE FROM \"customer\" WHERE \"id\" = ?");
            PreparedStatement updatePS = c.prepareStatement("UPDATE \"customer\" SET \"id\" = ?, \"current_order_id\" = ? WHERE \"id\" = ?");
            PreparedStatement getAllPS = c.prepareStatement("SELECT * FROM \"customer\"");

            @Override
            public Customer fromResultSet(ResultSet resultSet) throws SQLException {
                String id = resultSet.getString("id");
                String currentOrder = resultSet.getString("current_order_id");
                return new CustomerSQLImpl(id, new OrderImpl(currentOrder), new CustomerOldOrderDAO(c, id), orderDAO);
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
                if(o.hasCurrentOrder()) {
                    putPS.setString(2, o.getCurrentOrder().getId());
                } else {
                    putPS.setString(2, null);
                }
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
                updatePS.setString(2, o.getCurrentOrder().getId());
                updatePS.setString(3, key);
                return null;
            }

            @Override
            public PreparedStatement getAll() {
                return getAllPS;
            }
        });
    }
}