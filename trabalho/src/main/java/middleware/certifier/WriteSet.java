package middleware.certifier;

import java.io.Serializable;

public interface WriteSet<K> extends Serializable {
    void add(String key);
    boolean intersects(WriteSet<K> ws);
    K getSet();
}
