package middleware.certifier;

public interface OperationSet<K> {
    void add(String key);
    boolean intersects(OperationSet<K> ws);
    K getSet();
}
