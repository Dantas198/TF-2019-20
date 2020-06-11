package middleware.certifier;

public interface StateUpdater<K, V> {
    void put(String tag, K key, V values);
    void read(String tag, K key);
}
