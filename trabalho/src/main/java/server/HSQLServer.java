package server;

import org.hsqldb.server.Server;

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
        this.setDatabasePath(this.idx, "file:db/" + databaseName + ";user=user;password=password;hsqldb.lock_file=false;hsqldb.sqllog=2;hsqldb.write_delay=false;sql.syntax_mys=true");
        this.idx++;
        this.setSilent(false);
    }

    public static void main(String[] args) {
        new HSQLServer("1").start();
    }
}
