package middleware.message.replication;

import java.util.Map;
import java.util.Set;

public interface Certifiable<K>{
    // Maps table name to Bit Write Set
    Map<String, K> getSets();
    Set<String> getTables();
    long getStartTimestamp();
}
