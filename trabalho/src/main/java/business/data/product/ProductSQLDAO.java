package business.data.product;

import business.data.DAOPS;
import business.data.SQLDAO;
import business.product.Product;
import business.product.ProductImpl;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductSQLDAO extends SQLDAO<String, Product> {
    public ProductSQLDAO(Connection c) throws SQLException {
        super(new DAOPS<String, Product>() {
            PreparedStatement getPS = c.prepareStatement("SELECT * FROM \"product\" WHERE \"name\" = ?");
            PreparedStatement putPS = c.prepareStatement("INSERT INTO \"product\" (\"name\", \"price\", \"description\") VALUES (?, ?, ?)");
            PreparedStatement deletePS = c.prepareStatement("DELETE FROM \"product\" WHERE \"name\" = ?");
            PreparedStatement updatePS = c.prepareStatement("UPDATE \"product\" SET \"name\" = ?,  \"price\" = ?, \"description\" = ? WHERE \"name\" = ?");
            PreparedStatement getAllPS = c.prepareStatement("SELECT * FROM \"product\"");

            @Override
            public Product fromResultSet(ResultSet resultSet) throws SQLException {
                String name = resultSet.getString("name");
                float price = resultSet.getFloat("price");
                String description = resultSet.getString("description");
                return new ProductImpl(name, price, description);
            }

            @Override
            public String getKey(ResultSet resultSet) throws SQLException {
                return resultSet.getString("name");
            }

            @Override
            public PreparedStatement get(String key) throws SQLException {
                getPS.setString(1, key);
                return getPS;
            }

            @Override
            public PreparedStatement put(Product o) throws SQLException {
                putPS.setString(1, o.getName());
                putPS.setFloat(2, o.getPrice());
                putPS.setString(3, o.getDescription());
                return putPS;
            }

            @Override
            public PreparedStatement delete(String key) throws SQLException {
                deletePS.setString(1, key);
                return deletePS;
            }

            @Override
            public PreparedStatement update(String key, Product o) throws SQLException {
                updatePS.setString(1, o.getName());
                updatePS.setFloat(2, o.getPrice());
                updatePS.setString(3, o.getDescription());
                updatePS.setString(4, key);
                return null;
            }

            @Override
            public PreparedStatement getAll() throws SQLException {
                return getAllPS;
            }
        });
    }
}
