package server;

import business.data.DBInitialization;
import org.hsqldb.server.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class HSQLServer extends Server {
    int idx = 0;

    public HSQLServer() {
        super();
    }

    public HSQLServer(String databaseName) {
        super();
        addDatabase(databaseName);
    }

    public void addDatabase(String databaseName) {
        this.setDatabaseName(this.idx, "");
        this.setDatabasePath(this.idx, "file:db/" + databaseName + ";user=user;password=password;hsqldb.lock_file=false;hsqldb.sqllog=2;hsqldb.write_delay=false;sql.syntax_mys=true;hsqldb.log_size=1;;hsqldb.script_format=3;");
        this.idx++;
        this.setSilent(false);
    }

    public static void main(String[] args) {
        try {
            System.out.println("What is my id?");
            int id = new Scanner(System.in).nextInt();
            HSQLServer server = new HSQLServer();
            int port = 9000 + id;
            server.setPort(port);
            server.addDatabase("Server" + id);
            server.start();
            Connection connection = DriverManager.getConnection("jdbc:hsqldb:hsql://localhost:" + port + ";user=user;password=password");
            DBInitialization dbInit = new DBInitialization(connection);
            if (!dbInit.exists()) {
                dbInit.init();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
