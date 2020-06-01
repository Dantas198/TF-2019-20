package business.data;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

public class DBInitialization {
    private Connection c;

    public DBInitialization(Connection c) {
        this.c = c;
    }

    public void init() throws SQLException {
        c.prepareStatement("DROP SCHEMA PUBLIC CASCADE").execute();
        // Customer table
        c.prepareStatement("DROP TABLE IF EXISTS \"customer\";\n").execute();
        c.prepareStatement("CREATE TABLE \"customer\" (\n" +
                "    \"id\" varchar(255),\n" +
                "    \"current_order_id\" varchar(255),\n" +
                "    PRIMARY KEY(\"id\")\n" +
                "); ").execute();
        // Order table
        c.prepareStatement("DROP TABLE IF EXISTS \"order\";\n").execute();
        c.prepareStatement("CREATE TABLE \"order\" (\n" +
                "    \"id\" varchar(255),\n" +
                "    \"customer_id\" varchar(255),\n" +
                "    \"timestamp\" TIMESTAMP,\n" +
                "    PRIMARY KEY(\"id\"),\n" +
                "    FOREIGN KEY (\"customer_id\") REFERENCES \"customer\"(\"id\")" +
                "); ").execute();
        c.prepareStatement("ALTER TABLE \"customer\"\n" +
                "   ADD CONSTRAINT FK_customer_current_order_id_order_id\n" +
                "   FOREIGN KEY (\"current_order_id\") REFERENCES \"order\"(\"id\")").execute();
        // Product table
        c.prepareStatement("DROP TABLE IF EXISTS \"product\";\n").execute();
        c.prepareStatement("CREATE TABLE \"product\" (\n" +
                "    \"name\" varchar(255),\n" +
                "    \"description\" varchar(255),\n" +
                "    \"price\" int,\n" +
                "    \"stock\" int,\n" +
                "    PRIMARY KEY(\"name\")\n" +
                "); ").execute();
        // Order <-> Product table
        c.prepareStatement("DROP TABLE IF EXISTS \"order_product\";\n").execute();
        c.prepareStatement("CREATE TABLE \"order_product\" (\n" +
                "    \"order_id\" varchar(255),\n" +
                "    \"product_name\" varchar(255),\n" +
                "    \"quantity\" int,\n" +
                "    PRIMARY KEY(\"order_id\", \"product_name\"),\n" +
                "    FOREIGN KEY (\"order_id\") REFERENCES \"order\"(\"id\"),\n" +
                "    FOREIGN KEY (\"product_name\") REFERENCES \"product\"(\"name\")" +
                "); ").execute();
    }

    public boolean exists() throws SQLException{
        return c.getMetaData().getSchemas().next();
    }
}
