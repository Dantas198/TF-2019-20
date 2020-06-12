package business.data;

import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class DBInitialization {
    private Connection c;

    public DBInitialization(Connection c) {
        this.c = c;
    }

    public void init() throws SQLException {
        c.prepareCall("DROP SCHEMA PUBLIC CASCADE").execute();
        // Customer table
        c.prepareCall("DROP TABLE IF EXISTS \"customer\";\n").execute();
        c.prepareCall("CREATE TABLE \"customer\" (\n" +
                "    \"id\" varchar(255),\n" +
                "    \"current_order_id\" varchar(255),\n" +
                "    PRIMARY KEY(\"id\")\n" +
                "); ").execute();
        // Order table
        c.prepareCall("DROP TABLE IF EXISTS \"order\";\n").execute();
        c.prepareCall("CREATE TABLE \"order\" (\n" +
                "    \"id\" varchar(255),\n" +
                "    \"customer_id\" varchar(255),\n" +
                "    \"timestamp\" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" +
                "    PRIMARY KEY(\"id\"),\n" +
                "    FOREIGN KEY (\"customer_id\") REFERENCES \"customer\"(\"id\")" +
                "); ").execute();
        c.prepareCall("ALTER TABLE \"customer\"\n" +
                "   ADD CONSTRAINT FK_customer_current_order_id_order_id\n" +
                "   FOREIGN KEY (\"current_order_id\") REFERENCES \"order\"(\"id\") ON DELETE SET NULL").execute();
        // Product table
        c.prepareCall("DROP TABLE IF EXISTS \"product\";\n").execute();
        c.prepareCall("CREATE TABLE \"product\" (\n" +
                "    \"name\" varchar(255),\n" +
                "    \"description\" varchar(255),\n" +
                "    \"price\" float,\n" +
                "    \"stock\" int,\n" +
                "    PRIMARY KEY(\"name\")\n" +
                "); ").execute();
        // Order <-> Product table
        c.prepareCall("DROP TABLE IF EXISTS \"order_product\";\n").execute();
        c.prepareCall("CREATE TABLE \"order_product\" (\n" +
                "    \"order_id\" varchar(255),\n" +
                "    \"product_name\" varchar(255),\n" +
                "    \"quantity\" int,\n" +
                "    PRIMARY KEY(\"order_id\", \"product_name\"),\n" +
                "    FOREIGN KEY (\"order_id\") REFERENCES \"order\"(\"id\"),\n" +
                "    FOREIGN KEY (\"product_name\") REFERENCES \"product\"(\"name\")" +
                "); ").execute();
        c.prepareCall("DROP TABLE IF EXISTS \"__certifier\";\n").execute();
        c.prepareCall("CREATE TABLE \"__certifier\" (\n" +
                "    \"timestamp\" bigint,\n" +
                "    \"table_name\" varchar(255),\n" +
                "    \"keys\" longvarchar,\n" +
                "    PRIMARY KEY(\"timestamp\", \"table_name\")\n" +
                "); ").execute();
    }

    public void populateProduct() throws SQLException {
        PreparedStatement ps = c.prepareStatement("INSERT INTO \"product\" (\n" +
                "    \"name\",\n" +
                "    \"description\",\n" +
                "    \"price\",\n" +
                "    \"stock\")\n" +
                "    VALUES (?, ?, ?, ?);"
        );
        List<String> products = Arrays.asList("Queijo", "Fiambre", "Manteiga", "Presunto");
        Random random = new Random();
        for (String product:
             products) {
            ps.setString(1, product);
            ps.setString(2, product);
            ps.setInt(3, random.nextInt(10));
            ps.setInt(4, random.nextInt(10));
            ps.execute();
        }
    }

    public boolean exists() throws SQLException{
        List<String> tables = Arrays.asList("customer", "order", "product", "order_product", "__certifier");
        final DatabaseMetaData metaData = c.getMetaData();
        for (String table:
             tables) {
            if(!metaData.getTables(null, null, table, null).next())
                return false;
        }
        return true;
    }
}
