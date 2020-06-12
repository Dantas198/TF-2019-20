package middleware.certifier;

import java.util.Map;
import java.util.Set;

public interface StateUpdates<K, V> extends StateUpdater<K, V> {
    Map<String, OperationalSets> getSets();
    Set<TaggedObject<String, V>> getAllUpdates();
}
