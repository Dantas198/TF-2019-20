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

    public static void main(String[] args) throws Exception {
        MultiGandaGotaServerInitializer init = new MultiGandaGotaServerInitializer();

        Map<Integer, HSQLServer> databases = new HashMap<>();
        Map<Integer, Server> servers = new HashMap<>();

        Scanner scanner = new Scanner(System.in);

        System.out.println("Número de servidores:");
        int numServers = scanner.nextInt();
        int i = 0;
        for (; i < numServers; i++) {
            String serverName = "Server" + i;
            HSQLServer dbServer = init.initDatabase(serverName, 9000 + i);
            databases.put(i, dbServer);
            Connection connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:" + (9000 + i), "user", "password");
            servers.put(i, init.initServer(serverName, 6000 + i, connection, numServers, "db/" + serverName + ".log", "timestamp/" + serverName + ".timestamp"));
        }


        while (true) {
            try {
                String[] strs = scanner.nextLine().split(" ");
                switch (strs[0]) {
                    case "end": {
                        return;
                    }
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
                        servers.put(i, init.initServer(newServerName, 6000 + idx, connection, numServers,
                                "db/Server" + idx + ".log", "timestamp/" + serverName));
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
}
