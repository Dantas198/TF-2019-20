import middleware.Server;
import org.junit.Test;
import server.HSQLServer;
import server.MultiGandaGotaServerInitializer;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class TestesIntensivosTest {

    public static void main(String[] args) throws SQLException {
        MultiGandaGotaServerInitializer init = new MultiGandaGotaServerInitializer();

        Map<Integer, HSQLServer> databases = new HashMap<>();
        Map<Integer, Server> servers = new HashMap<>();

        Scanner scanner = new Scanner(System.in);

        System.out.println("Número de servidores:");
        int numServers = scanner.nextInt();
        int i = 0;
        for (; i < numServers ; i++) {
            String serverName = "Server" + i;
            HSQLServer dbServer = init.initDatabase(serverName, 9000 + i);
            databases.put(i, dbServer);
            servers.put(i, init.initServer(serverName, 6000 + i,
                    "jdbc:hsqldb:hsql://localhost:" + (9000 + i) + ";user=user;password=password",
                    numServers,
                    "db/" + serverName + ".log", "timestamp/" + serverName + ".timestamp"));
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
                        servers.put(i, init.initServer(newServerName, 6000 + idx, "jdbc:hsqldb:hsql://localhost:" + (9000 + idx) + ";user=user;password=password", numServers,
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


    @Test
    public void testeIntensivo() throws SQLException {
        MultiGandaGotaServerInitializer init = new MultiGandaGotaServerInitializer();

        Map<Integer, HSQLServer> databases = new HashMap<>();
        Map<Integer, Server> servers = new HashMap<>();



        System.out.println("Número de servidores:");
        int numServers = 3;
        int i = 0;
        for (; i < numServers ; i++) {
            String serverName = "Server" + i;
            HSQLServer dbServer = init.initDatabase(serverName, 9000 + i);
            databases.put(i, dbServer);
            servers.put(i, init.initServer(serverName, 6000 + i, "jdbc:hsqldb:hsql://localhost:" + (9000 + i) + ";user=user;password=password", numServers, "db/" + serverName + ".log", "timestamp/" + serverName + ".timestamp"));
        }


    /*
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
    */
    }

}
