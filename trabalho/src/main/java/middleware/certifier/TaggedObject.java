package middleware.certifier;

import java.io.Serializable;

public class TaggedObject<K, V> implements Serializable {
    private String tag;
    private K key;
    private V object;

    public TaggedObject(String tag, K key, V object) {
        this.tag = tag;
        this.key = key;
        this.object = object;
    }

    public String getTag() {
        return tag;
    }

    public K getKey() {
        return this.key;
    }

    public V getObject() {
        return object;
    }
}
