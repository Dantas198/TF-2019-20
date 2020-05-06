package business.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public abstract class DAOMap<K, V> implements Map<K, V> {
    private DAOSet<Map.Entry<K, V>> entrySet;

    public DAOMap(DAOSet<Entry<K, V>> entrySet) {
        this.entrySet = entrySet;
    }

    @Override
    public int size() {
        return this.entrySet.size();
    }

    @Override
    public boolean isEmpty() {
        return this.entrySet.isEmpty();
    }

    @Override
    public boolean containsKey(Object o) {
        return materialize().containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        return materialize().containsValue(o);
    }

    @Override
    public V get(Object o) {
        return materialize().get(o);
    }

    @Override
    public V put(K k, V v) {
        if(this.entrySet.add(Map.entry(k, v))) {
            return materialize().put(k, v);
        }
        return null;
    }

    @Override
    public V remove(Object o) {
        if(this.entrySet.remove(o)) {
            return materialize().remove(o);
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        for (Entry<? extends K, ? extends V> entry:
             map.entrySet()) {
            this.entrySet.add((Entry<K, V>) entry);
        }
    }

    @Override
    public void clear() {
        this.entrySet.clear();
    }

    @Override
    public Set<K> keySet() {
        Set<K> set = new HashSet<>();
        for (Entry<K, V> entry:
                entrySet) {
            set.add(entry.getKey());
        }
        return set;
    }

    @Override
    public Collection<V> values() {
        Collection<V> collection = new ArrayList<>();
        for (Entry<K, V> entry:
             entrySet) {
            collection.add(entry.getValue());
        }
        return collection;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return entrySet;
    }

    private Map<K, V> materialize() {
        Map<K, V> map = new HashMap<>();
        for (Entry<K, V> entry:
             entrySet) {
            map.put(entry.getKey(), entry.getValue());
        }
        return map;
    }
}
