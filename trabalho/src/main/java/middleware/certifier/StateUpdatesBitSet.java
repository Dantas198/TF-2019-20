package middleware.certifier;

import java.util.*;

public class StateUpdatesBitSet<V> implements StateUpdates<String, V> {

    private Map<String, BitWriteSet> wss;
    private Map<String, Set<V>> objects;

    public StateUpdatesBitSet() {
        this.wss = new HashMap<>();
        this.objects = new HashMap<>();
    }

    @Override
    public Map<String, BitWriteSet> getWriteSets() {
        return this.wss;
    }

    @Override
    public Set<String> getTags() {
        return this.objects.keySet();
    }

    @Override
    public Map<String, Set<V>> getAllUpdates() {
        return this.objects;
    }

    @Override
    public Set<V> getObjects(String tag) {
        return this.objects.get(tag);
    }

    @Override
    public void put(String table, String key, V value) {
        this.wss.getOrDefault(table, new BitWriteSet()).add(key);
        this.objects.getOrDefault(table, new HashSet<>()).add(value);
    }
}
