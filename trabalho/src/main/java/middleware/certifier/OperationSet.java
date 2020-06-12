package middleware.certifier;

import java.util.BitSet;

public interface OperationSet {
    void add(String key);
    boolean intersects(BitOperationSet ws);
    BitSet getSet();
}
