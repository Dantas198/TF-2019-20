package middleware.certifier;

import java.util.*;

public class StateUpdatesBitSet<V> implements StateUpdates<String, V> {

    private Map<String, BitOperationSet> writeSets;
    private Map<String, BitOperationSet> readSets;
    private Set<TaggedObject<String, V>> objects;

    public StateUpdatesBitSet() {
        this.writeSets = new HashMap<>();
        this.readSets = new HashMap<>();
        this.objects = new LinkedHashSet<>();
    }

    public Map<String, OperationalSets> getSets() {
        Set<String> keys = new HashSet<>();
        keys.addAll(writeSets.keySet());
        keys.addAll(readSets.keySet());
        Map<String, OperationalSets> result = new HashMap<>();
        for (String tag: keys) {
            OperationalSets operationalSets = new OperationalSets(writeSets.get(tag), readSets.get(tag));
            result.put(tag, operationalSets);
        }
        return result;
    }

    @Override
    public Set<TaggedObject<String, V>> getAllUpdates() {
        return this.objects;
    }

    @Override
    public void put(String tag, String key, V value) {
        this.writeSets.computeIfAbsent(tag, k -> new BitOperationSet()).add(key);
        objects.add(new TaggedObject<>(tag, key, value));
    }

    @Override
    public void read(String tag, String key) {
        this.readSets.computeIfAbsent(tag, k -> new BitOperationSet()).add(key);
    }
}
