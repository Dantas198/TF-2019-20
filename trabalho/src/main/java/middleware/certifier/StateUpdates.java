package middleware.certifier;

import java.util.Map;
import java.util.Set;

public interface StateUpdates<K, V> extends StateUpdater<K, V> {
    Map<String, BitOperationSet> getWriteSets();
    Map<String, BitOperationSet> getReadSets();
    Set<TaggedObject<String, V>> getAllUpdates();
}
