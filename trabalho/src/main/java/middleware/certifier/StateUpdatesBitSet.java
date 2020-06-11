package middleware.certifier;

import java.util.*;

public class StateUpdatesBitSet<V> implements StateUpdates<String, V> {

    private final Map<String, BitWriteSet> writeSets;
    private final Map<String, BitWriteSet> readSets;
    private final Set<TaggedObject<String, V>> objects;

    public StateUpdatesBitSet() {
        this.writeSets = new HashMap<>();
        this.readSets = new HashMap<>();
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
        // Allows duplicates in read and write Set
        this.readSets.computeIfAbsent(tag, k -> new BitWriteSet()).add(key);
    }
}
