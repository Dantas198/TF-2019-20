package middleware.certifier;

import java.util.Map;
import java.util.Set;

public interface StateUpdates<K, V> extends StateUpdater<K, V> {
    Map<String, BitWriteSet> getWriteSets();
    Set<String> getTags();
    Map<String, Set<V>> getAllUpdates();
    Set<V> getObjects(String tag);
}
