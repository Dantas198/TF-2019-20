package middleware.message.replication;

import middleware.Certifier.BitWriteSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Certifiable{
    // Maps table name to Bit Write Set
    Map<String, BitWriteSet> getWriteSets();
    Set<String> getTables();
    long getStartTimestamp();
}
