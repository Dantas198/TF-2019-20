package middleware.certifier;

public interface WriteSet<K> {
    void add(String key);
    boolean intersects(WriteSet<K> ws);
    K getSet();
}
