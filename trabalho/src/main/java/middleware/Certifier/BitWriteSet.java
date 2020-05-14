package middleware.Certifier;

import java.io.Serializable;
import java.util.Arrays;
import java.util.BitSet;

/**
 * WriteSet used to certify writes.
 */
public class BitWriteSet implements Serializable {

    private BitSet set;

    public BitWriteSet() {
        this(1024);
    }

    public BitWriteSet(int nbits) {
        this.set = new BitSet(nbits);
    }

    public void add(byte[] key) {
        int index = Arrays.hashCode(key) % set.size();
        set.set(index, true);
    }

    public boolean intersects(BitWriteSet set) {
        return this.set.intersects(set.getSet());
    }

    public BitSet getSet() {
        return set;
    }
}
