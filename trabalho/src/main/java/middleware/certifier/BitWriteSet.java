package middleware.certifier;

import java.io.Serializable;
import java.util.BitSet;

/**
 * WriteSet used to certify writes.
 */

public class BitWriteSet implements Serializable, WriteSet<BitSet>  {
    private BitSet set;

    public BitWriteSet() {
        this(1024);
    }

    public BitWriteSet(int nbits) {
        this.set = new BitSet(nbits);
    }

    @Override
    public void add(String key) {
        int index = (key.hashCode() & 0x7fffffff) % set.size();
        set.set(index, true);
    }

    @Override
    public boolean intersects(WriteSet<BitSet> set) {
        return this.set.intersects(set.getSet());
    }

    @Override
    public BitSet getSet() { return set;}
}
