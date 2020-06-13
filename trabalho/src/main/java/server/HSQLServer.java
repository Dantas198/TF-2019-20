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
        this.setDatabasePath(this.idx, "file:db/" + databaseName + ";user=user;password=password;hsqldb.lock_file=false;hsqldb.sqllog=2;hsqldb.write_delay=false;sql.syntax_mys=true;hsqldb.log_size=1;hsqldb.script_format=3;");
        this.idx++;
        this.setSilent(false);
    }
}
