package middleware.certifier;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class NormalOperationSet implements Serializable, OperationSet<Set<String>> {
    private HashSet<String> set;

    public NormalOperationSet() {
        this(1024);
    }

    public NormalOperationSet(int nbits) {
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
    public boolean intersects(OperationSet<Set<String>> ws) {
        NormalOperationSet nws = (NormalOperationSet) ws;
        for(String s : nws.getSet())
            if (this.set.contains(s))
                return true;
        return false;
    }
}
