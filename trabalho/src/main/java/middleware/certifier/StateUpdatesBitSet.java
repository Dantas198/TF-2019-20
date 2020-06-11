package middleware.certifier;

import java.util.*;

public class StateUpdatesBitSet<V> implements StateUpdates<String, V> {

    private Map<String, BitWriteSet> writeSets;
    private Map<String, BitWriteSet> readSets;
    private Set<TaggedObject<String, V>> objects;

    public StateUpdatesBitSet() {
        this.writeSets = new HashMap<>();
        this.objects = new LinkedHashSet<>();
    }

    @Override
    public Map<String, BitWriteSet> getWriteSets() {
        return this.writeSets;
    }

    @Override
    public Map<String, BitWriteSet> getReadSets() {
        return this.readSets;
    }

    @Override
    public Set<TaggedObject<String, V>> getAllUpdates() {
        return this.objects;
    }

    @Override
    public void put(String tag, String key, V value) {
        this.writeSets.computeIfAbsent(tag, k -> new BitWriteSet()).add(key);
        objects.add(new TaggedObject<>(tag, key, value));
    }

    @Override
    public void read(String tag, String key) {
        this.readSets.computeIfAbsent(tag, k -> new BitWriteSet()).add(key);
    }
}
