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
        Map<Integer, Connection> databases = new HashMap<>();
        Map<Integer, Server> servers = new HashMap<>();
        int i = 0;
        int numServers = Integer.parseInt(args[0]);
        for (; i < (args.length < 1 ? 1 : Integer.parseInt(args[0])); i++) {
            String serverName = "Server" + i;
            Connection connection = initDatabase(serverName, 9000 + i);
            databases.put(i, connection);
            servers.put(i, initServer(serverName, 6000 + i, connection, numServers, "db/" + serverName + ".log"));
        }
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                String[] strs = scanner.nextLine().split(" ");
                switch (strs[0]) {
                    case "add": {
                        String serverName = "Server" + i;
                        Connection connection = initDatabase(serverName, 9000 + i);
                        databases.put(i, connection);
                        servers.put(i, initServer(serverName, 6000 + i, connection,  numServers, "db/" + serverName + ".log"));
                        i++;
                    }
                    break;
                    case "shutdown": {
                        int idx = Integer.parseInt(strs[1]);
                        servers.remove(idx).stop();
                    }
                    break;
                    case "reboot": {
                        int idx = Integer.parseInt(strs[1]);
                        String serverName = "Server" + i++;
                        servers.put(i, initServer(serverName, 6000 + idx, databases.get(idx), numServers, "db/Server" + idx + ".log"));
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

    public static Connection initDatabase(String serverName, int port) throws SQLException {
        HSQLServer server = new HSQLServer();
        server.setPort(port);
        server.addDatabase(serverName);
        server.start();
        Connection connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:" + port, "user", "password");
        DBInitialization dbInit = new DBInitialization(connection);
        return connection;
    }

    public static Server initServer(String serverName, int atomixPort, Connection connection, int totalServerCount, String logPath) {
        try {
            Server server = new GandaGotaServerImpl(4803, serverName, atomixPort, connection, totalServerCount, logPath);
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
