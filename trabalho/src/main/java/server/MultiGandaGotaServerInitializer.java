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
    public static void main(String[] args) throws Exception {
        Map<Integer, HSQLServer> databases = new HashMap<>();
        Map<Integer, Server> servers = new HashMap<>();
        int i = 0;
        int numServers = Integer.parseInt(args[0]);
        for (; i < (args.length < 1 ? 1 : Integer.parseInt(args[0])); i++) {
            String serverName = "Server" + i;
            HSQLServer dbServer = initDatabase(serverName, 9000 + i);
            databases.put(i, dbServer);
            Connection connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:" + (9000 + i), "user", "password");
            servers.put(i, initServer(serverName, 6000 + i, connection, numServers, "db/" + serverName + ".log", "timestamp/" + serverName + ".timestamp"));
        }
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                String[] strs = scanner.nextLine().split(" ");
                switch (strs[0]) {
                    case "shutdown": {
                        int idx = Integer.parseInt(strs[1]);
                        databases.get(idx).shutdown();
                    }
                    break;
                    case "stop": {
                        int idx = Integer.parseInt(strs[1]);
                        databases.get(idx).stop();
                    }
                    break;
                    case "reboot": {
                        int idx = Integer.parseInt(strs[1]);
                        String serverName = "Server" + idx;
                        String newServerName = "Server" + i++;
                        databases.get(idx).start();
                        Connection connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:" + (9000 + idx), "user", "password");
                        servers.put(i, initServer(newServerName, 6000 + idx, connection, numServers, "db/Server" + idx + ".log", "timestamp/" + serverName));
                    }
                    break;
                    case "active": {
                        System.out.println("Servers: " + servers.keySet());
                        System.out.println("Databases: " + databases.keySet());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static HSQLServer initDatabase(String serverName, int port) throws SQLException {
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

    public static Server initServer(String serverName, int atomixPort, Connection connection, int totalServerCount, String logPath, String timestampPath) {
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
