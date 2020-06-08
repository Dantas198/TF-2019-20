package server;

import business.data.DBInitialization;

import java.sql.DriverManager;

public class MultiGandaGotaServerInitializer {
    public static void main(String[] args) throws Exception {
        HSQLServer server = new HSQLServer();
        for (int i = 0; i < (args.length < 1 ? 1 : Integer.parseInt(args[0])); i++) {
            String serverName = "Server" + i;
            server.addDatabase(serverName);
        }
        server.start();
        for (int i = 0; i < (args.length < 1 ? 1 : Integer.parseInt(args[0])); i++) {
            String serverName = "Server" + i;
            DBInitialization dbInit = new DBInitialization(DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:9001/" + serverName, "user", "password"));
            if(!dbInit.exists()) {
                dbInit.init();
                System.out.println("Database initialized " + i);
            }
            int finalI = i;
            new Thread(() -> {
                try {
                    new GandaGotaServerImpl(4803, serverName, 6000 + finalI).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}
