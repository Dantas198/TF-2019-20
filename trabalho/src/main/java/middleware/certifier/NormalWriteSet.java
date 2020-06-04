package middleware.certifier;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class NormalWriteSet implements Serializable, WriteSet<Set<String>> {
    private HashSet<String> set;

    public NormalWriteSet() {
        this(1024);
    }

    public NormalWriteSet(int nbits) {
        this.set = new HashSet<>(nbits);
    }

    @Override
    public void add(String key) {
        this.set.add(key);
    }

    @Override
    public Set<String> getSet() {
        return set;
    }

    @Override
    public boolean intersects(WriteSet<Set<String>> ws) {
        NormalWriteSet nws = (NormalWriteSet) ws;
        for(String s : nws.getSet())
            if (this.set.contains(s))
                return true;
        return false;
    }
}
