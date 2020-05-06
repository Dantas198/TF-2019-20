package business.data.product;

import business.data.DAOMap;
import business.data.DAOSet;
import business.data.DAOSetPS;
import business.product.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class OrderProductDAO extends DAOMap<Product, Integer> {
    public OrderProductDAO(Connection c, String orderId) throws SQLException {
        super(new DAOSet<>(new DAOSetPS<>() {
            private ProductDAO productDAO = new ProductDAO();

            @Override
            public Entry<Product, Integer> fromResultSet(ResultSet resultSet) throws SQLException {
                String productId = resultSet.getString("product_id");
                int quantity = resultSet.getInt("quantity");
                return Map.entry(productDAO.get(productId), quantity);
            }

            @Override
            public PreparedStatement getAll() throws SQLException {
                PreparedStatement ps = c.prepareStatement("SELECT * FROM \"order_product\" WHERE \"order_id\" = ?");
                ps.setString(1, orderId);
                return ps;
            }

            @Override
            public PreparedStatement size() throws SQLException {
                PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM \"order_product\" WHERE \"order_id\" = ?");
                ps.setString(1, orderId);
                return ps;
            }

            @Override
            public PreparedStatement add(Entry<Product, Integer> o) throws SQLException {
                PreparedStatement ps = c.prepareStatement("INSERT INTO \"order_product\" (\"order_id\", \"product_name\", \"quantity\") VALUES (?, ?, ?)");
                ps.setString(1, orderId);
                ps.setString(2, o.getKey().getName());
                ps.setInt(3, o.getValue());
                return ps;
            }

            @Override
            public PreparedStatement remove(Object o) throws SQLException {
                if(o instanceof Entry) {
                    o = ((Entry) o).getKey();
                }
                if(o instanceof Product) {
                    Product product = (Product) o;
                    PreparedStatement ps = c.prepareStatement("DELETE FROM \"order_product\" WHERE \"order_id\" = ? AND \"product_name\" = ?");
                    ps.setString(1, orderId);
                    ps.setString(2, product.getName());
                    return ps;
                } else {
                    return c.prepareStatement("");
                }
            }

            @Override
            public PreparedStatement clear() throws SQLException {
                PreparedStatement ps = c.prepareStatement("DELETE FROM \"order_product\" WHERE \"order_id\" = ?");
                ps.setString(1, orderId);
                return ps;
            }

            @Override
            public PreparedStatement empty() throws SQLException {
                PreparedStatement ps = c.prepareStatement("SELECT 1 FROM \"order_product\" WHERE \"order_id\" = ?");
                ps.setString(1, orderId);
                return ps;
            }

            @Override
            public PreparedStatement contains(Object o) throws SQLException {
                if (o instanceof Product) {
                    PreparedStatement ps = c.prepareStatement("SELECT 1 FROM \"order_product\" WHERE \"order_id\" = ? AND \"product_name\" = ?");
                    ps.setString(1, orderId);
                    ps.setString(2, ((Product) o).getName());
                    return ps;
                } else if (o instanceof Entry) {
                    PreparedStatement ps = c.prepareStatement("SELECT 1 FROM \"order_product\" WHERE \"order_id\" = ? AND \"product_name\" = ? AND \"quantity\" = ?");
                    ps.setString(1, orderId);
                    ps.setString(2, ((Product) ((Entry) o).getKey()).getName());
                    ps.setInt(3, (int) ((Entry) o).getValue());
                    return ps;
                } else {
                    return c.prepareStatement("");
                }
            }
        }));
    }
}
