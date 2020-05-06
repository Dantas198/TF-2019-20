package business.data.customer;

import business.customer.Customer;
import business.customer.CustomerImpl;
import business.data.DAOPS;
import business.data.SQLDAO;

import java.sql.*;

public class CustomerSQLDAO extends SQLDAO<String, Customer> {
    public CustomerSQLDAO(Connection c) throws SQLException {
        super(new DAOPS<String, Customer>() {
            PreparedStatement getPS = c.prepareStatement("SELECT * FROM \"customer\" WHERE \"id\" = ?");
            PreparedStatement putPS = c.prepareStatement("INSERT INTO \"customer\" (\"id\") VALUES (?)");
            PreparedStatement deletePS = c.prepareStatement("DELETE FROM \"customer\" WHERE \"id\" = ?");
            PreparedStatement updatePS = c.prepareStatement("UPDATE \"customer\" SET \"id\" = ? WHERE \"id\" = ?");
            PreparedStatement getAllPS = c.prepareStatement("SELECT * FROM \"customer\"");

            @Override
            public Customer fromResultSet(ResultSet resultSet) throws SQLException {
                String id = resultSet.getString("id");
                return new CustomerImpl(id, null, new CustomerOldOrderDAO(c, id));
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
                updatePS.setString(2, key);
                return null;
            }

            @Override
            public PreparedStatement getAll() throws SQLException {
                return getAllPS;
            }
        });
    }
}