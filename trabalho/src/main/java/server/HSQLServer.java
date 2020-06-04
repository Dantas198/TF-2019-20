package server;

import org.hsqldb.server.Server;

public class HSQLServer extends Server {
    public HSQLServer(String databaseName) {
        super();
        this.setDatabaseName(0, databaseName);
        this.setDatabasePath(0, "file:db/" + databaseName + ";user=user;password=password;hsqldb.lock_file=false;hsqldb.sqllog=2;sql.syntax_mys=true");
        this.setSilent(false);
    }

    public static void main(String[] args) {
        new HSQLServer("1").start();
    }
}
