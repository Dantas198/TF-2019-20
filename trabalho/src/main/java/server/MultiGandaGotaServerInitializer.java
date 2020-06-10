package server;

import business.data.DBInitialization;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class MultiGandaGotaServerInitializer {
    public static void main(String[] args) throws Exception {
        Map<Integer, Connection> databases = new HashMap<>();
        Map<Integer, Thread> servers = new HashMap<>();
        int i = 0;
        for (; i < (args.length < 1 ? 1 : Integer.parseInt(args[0])); i++) {
            String serverName = "Server" + i;
            Connection connection = initDatabase(serverName, 9000 + i, i == 0);
            databases.put(i, connection);
            servers.put(i, initServer(serverName, 6000 + i, connection));
        }
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String[] strs = scanner.nextLine().split(" ");
            switch (strs[0]) {
                case "add": {
                    String serverName = "Server" + i;
                    Connection connection = initDatabase(serverName, 9000 + i, false);
                    databases.put(i, connection);
                    servers.put(i, initServer(serverName, 6000 + i, connection));
                    i++;
                }
                break;
                case "shutdown": {
                    int idx = Integer.parseInt(strs[1]);
                    servers.remove(idx).interrupt();
                }
                break;
                case "reboot": {
                    int idx = Integer.parseInt(strs[1]);
                    String serverName = "Server" + idx;
                    servers.put(idx, initServer(serverName, 6000 + idx, databases.get(idx)));
                }
                break;
                case "active": {
                    System.out.println("Servers: " + servers.keySet());
                    System.out.println("Databases: " + databases.keySet());
                }
            }
        }
    }

    public static Connection initDatabase(String serverName, int port, boolean populate) throws SQLException {
        HSQLServer server = new HSQLServer();
        server.setPort(port);
        server.addDatabase(serverName);
        server.start();
        Connection connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:" + port, "user", "password");
        DBInitialization dbInit = new DBInitialization(connection);
        if(!dbInit.exists()) {
            dbInit.init();
            System.out.println("Database initialized " + serverName);
            if(populate) {
                dbInit.populateProduct();
                System.out.println("Product table populated ");
            }
        }
        return connection;
    }

    public static Thread initServer(String serverName, int atomixPort, Connection connection) {
        Thread thread = new Thread(() -> {
            try {
                new GandaGotaServerImpl(4803, serverName, atomixPort, connection).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
        return thread;
    }
}
