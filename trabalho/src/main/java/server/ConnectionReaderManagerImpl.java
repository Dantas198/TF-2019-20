package server;

import middleware.dbutils.ConnectionReaderManager;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionReaderManagerImpl extends ConnectionReaderManager<Connection> {
    private String dbConnectionUrl;

    public ConnectionReaderManagerImpl(int maxConnectionNumber, String dbConnectionUrl) {
        super(maxConnectionNumber);
        this.dbConnectionUrl = dbConnectionUrl;
    }

    @Override
    public Connection produceConnection() {
        try {
            return DriverManager.getConnection(this.dbConnectionUrl);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    }
}
