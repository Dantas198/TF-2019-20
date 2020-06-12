package server;

import business.data.DBInitialization;
import middleware.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MultiGandaGotaServerInitializer {

    public HSQLServer initDatabase(String serverName, int port) throws SQLException {
        HSQLServer server = new HSQLServer();
        server.setPort(port);
        server.addDatabase(serverName);
        server.start();
        Connection connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:" + port, "user", "password");
        DBInitialization dbInit = new DBInitialization(connection);
        if(!dbInit.exists()) {
            dbInit.init();
        }
        return server;
    }

    public Server initServer(String serverName,
                             int atomixPort,
                             Connection connection,
                             int totalServerCount,
                             String logPath,
                             String timestampPath) {
        try {
            Server server = new GandaGotaServerImpl(4803, serverName, atomixPort, connection, totalServerCount, logPath, timestampPath);
            new Thread(() -> {
                try {
                    server.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            return server;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
