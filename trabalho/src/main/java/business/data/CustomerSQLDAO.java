package business.data;

import business.customer.Customer;
import business.customer.CustomerImpl;

import java.sql.*;
import java.util.*;

public class CustomerSQLDAO implements DAO<String, Customer> {
    private Connection c;
    private PreparedStatement getPS;
    private PreparedStatement putPS;
    private PreparedStatement deletePS;
    private PreparedStatement updatePS;
    private PreparedStatement getAllPS;

    public CustomerSQLDAO(Connection c) {
        this(c, false);
    }

    public CustomerSQLDAO(Connection c, boolean createTables) {
        try {
            this.c = c;
            if(createTables) this.initializeTable();
            this.getPS = c.prepareStatement("SELECT * FROM \"customer\" WHERE \"id\" = ?");
            this.putPS = c.prepareStatement("INSERT INTO \"customer\" (\"id\") VALUES (?)");
            this.deletePS = c.prepareStatement("DELETE FROM \"customer\" WHERE \"id\" = ?");
            this.updatePS = c.prepareStatement("UPDATE \"customer\" SET \"id\" = ? WHERE \"id\" = ?");
            this.getAllPS = c.prepareStatement("SELECT * FROM \"customer\"");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }


    @Override
    public Customer get(String key) {
        try {
            getPS.setString(1, key);
            ResultSet rs = getPS.executeQuery();
            if(!rs.next()) return null;
            return newCustomer(rs);
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean put(Customer obj) {
        try {
            putPS.setString(1, obj.getId());
            return putPS.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean delete(String key) {
        if(c == null) return false;
        try {
            deletePS.setString(1, key);
            return deletePS.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean update(String key, Customer obj) {
        try {
            updatePS.setString(1, obj.getId());
            updatePS.setString(2, key);
            return updatePS.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public Map<String, Customer> getAll() {
        try {
            Map<String, Customer> map = new HashMap<>();
            ResultSet rs = this.getAllPS.executeQuery();
            while (rs.next()) {
                String id = rs.getString("id");
                Customer customer = newCustomer(rs);
                map.put(id, customer);
            }
            return map;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Customer newCustomer(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        return new CustomerImpl(id, null, new CustomerOldOrderDAO(c, id));
    }

    private void initializeTable() throws SQLException {
        c.prepareStatement("DROP TABLE IF EXISTS \"customer\";\n").execute();
        c.prepareStatement("CREATE TABLE \"customer\" (\n" +
                "    \"id\" varchar(255),\n" +
                "); ").execute();
        c.prepareStatement("CREATE TABLE \"order\" (\n" +
                "    \"id\" varchar(255),\n" +
                "    \"customer_id\" varchar(255),\n" +
                "    \"price\" decimal,\n" +
                "); ").execute();
    }
}