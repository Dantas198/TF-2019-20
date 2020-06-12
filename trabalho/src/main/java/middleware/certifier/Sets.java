package middleware.certifier;

public class Sets<V> {
    private OperationSet<V> writeSet;
    private OperationSet<V> readSet;

    public Sets(OperationSet<V> writeSet, OperationSet<V> readSet){
        this.writeSet = writeSet;
        this.readSet = readSet;
    }

    public OperationSet<V> getWriteSet(){
        return writeSet;
    }

    public OperationSet<V> getReadSet(){
        return readSet;
    }

    public boolean intersectRead(OperationSet<V> readSet){
        if (readSet == null || this.writeSet == null)
            return false;
        return readSet.intersects(this.writeSet);
    }

    public boolean intersectWrite(OperationSet<V> writeSet){
        if (writeSet == null)
            return false;
        else if (this.writeSet == null)
            return writeSet.intersects(this.readSet);
        else if (this.readSet == null)
            return writeSet.intersects(writeSet);

        return writeSet.intersects(writeSet) && writeSet.intersects(readSet);
    }

    public boolean intersect(Sets<V> set){
        return intersectWrite(set.getWriteSet()) && intersectRead(set.getReadSet());
    }
}
