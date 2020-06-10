package middleware.certifier;

import java.util.*;

public class StateUpdatesBitSet<V> implements StateUpdates<String, V> {

    private Map<String, BitWriteSet> wss;
    private Set<TaggedObject<String, V>> objects;

    public StateUpdatesBitSet() {
        this.wss = new HashMap<>();
        this.objects = new LinkedHashSet<>();
    }

    @Override
    public Map<String, BitWriteSet> getWriteSets() {
        return this.wss;
    }

    @Override
    public Set<TaggedObject<String, V>> getAllUpdates() {
        return this.objects;
    }

    @Override
    public void put(String table, String key, V value) {
        this.wss.computeIfAbsent(table, k -> new BitWriteSet()).add(key);
        objects.add(new TaggedObject<>(table, key, value));
    }
}
