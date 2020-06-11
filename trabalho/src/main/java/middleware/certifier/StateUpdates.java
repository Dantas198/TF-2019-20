package middleware.certifier;

import java.util.Map;
import java.util.Set;

public interface StateUpdates<K, V> extends StateUpdater<K, V> {
    Map<String, BitWriteSet> getWriteSets();
    Map<String, BitWriteSet> getReadSets();
    Set<TaggedObject<String, V>> getAllUpdates();
}
