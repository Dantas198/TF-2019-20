package business.data;

import business.customer.Customer;
import business.order.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CustomerOldOrderDAO extends DAOSet<Order> {
    String id;
    public CustomerOldOrderDAO(Connection c, String id) throws SQLException {
        super(c);
        this.id = id;
    }

    @Override
    Order fromResultSet(ResultSet resultSet) {
        return null;
    }

    @Override
    PreparedStatement getAllPS() throws SQLException {
        PreparedStatement ps = super.prepareStatement("SELECT * FROM \"order\" WHERE \"id\" = ?");
        ps.setString(1, id);
        return ps;
    }

    @Override
    PreparedStatement sizePS() throws SQLException {
        PreparedStatement ps = super.prepareStatement("SELECT COUNT(*) FROM \"order\" WHERE \"customer_id\" = ?");
        ps.setString(1, id);
        return ps;
    }

    @Override
    PreparedStatement addPS(Order o) throws SQLException {
        PreparedStatement ps = super.prepareStatement("INSERT INTO \"order\" (\"id\", \"customer_id\", \"price\") VALUES (?, ?, ?)");
        ps.setString(1, o.getId());
        ps.setString(2, id);
        ps.setFloat(3, o.getPrice());
        return ps;
    }

    @Override
    PreparedStatement removePS(Object o) throws SQLException {
        if(o instanceof Order) {
            Order order = (Order) o;
            PreparedStatement ps = super.prepareStatement("DELETE FROM \"order\" WHERE \"id\" = ?");
            ps.setString(1, order.getId());
            return ps;
        } else {
            return super.prepareStatement("");
        }
    }

    @Override
    PreparedStatement clearPS() throws SQLException {
        return super.prepareStatement("DELETE FROM \"order\" WHERE \"customer_id\" = ?");
    }

    @Override
    PreparedStatement emptyPS() throws SQLException {
        PreparedStatement ps = super.prepareStatement("SELECT 1 FROM \"order\" WHERE \"customer_id\" = ?");
        ps.setString(1, id);
        return ps;
    }

    @Override
    PreparedStatement containsPS(Object o) throws SQLException {
        if(o instanceof Order) {
            PreparedStatement ps = super.prepareStatement("SELECT 1 FROM \"order\" WHERE \"id\" = ?");
            ps.setString(1, ((Order) o).getId());
            return ps;
        } else {
            return super.prepareStatement("");
        }
    }
}
