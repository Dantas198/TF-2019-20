package server;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.util.Calendar;

public class CurrentOrderCleaner {
    private PreparedStatement preparedStatement;
    private long tmax;

    public CurrentOrderCleaner(Connection connection, Duration tmax) throws SQLException {
        this(connection, tmax, Calendar.getInstance());
    }

    public CurrentOrderCleaner(Connection connection, Duration tmax, Calendar calendar) throws SQLException {
        this.preparedStatement = connection.prepareStatement("DELETE FROM \"order\" WHERE \"timestamp\" < ? AND \"id\" IN (SELECT \"current_order_id\" FROM \"customer\" WHERE \"order\".\"customer_id\" = \"customer\".\"id\")");
        this.tmax = tmax.toMillis();
    }

    public boolean clean(long currentTime) throws SQLException {
        long threshold = currentTime - tmax;
        Calendar debugCalender = Calendar.getInstance();
        debugCalender.setTimeInMillis(threshold);
        System.out.println(debugCalender.getTime());
        this.preparedStatement.setTimestamp(1, new Timestamp(threshold));
        return this.preparedStatement.execute();
    }
}
