package middleware.message.replication;

import middleware.certifier.BitWriteSet;
import middleware.certifier.WriteSet;

import java.util.Map;
import java.util.Set;

public interface Certifiable<K>{
    // Maps table name to Bit Write Set
    Map<String, K> getWriteSets();
    Set<String> getTables();
    long getStartTimestamp();
}
