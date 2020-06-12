package middleware.message.replication;

import middleware.certifier.OperationalSets;

import java.util.Map;
import java.util.Set;

public interface Certifiable{
    // Maps table name to Bit Write Set
    Map<String, OperationalSets> getSets();
    Set<String> getTables();
    long getStartTimestamp();
}
