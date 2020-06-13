package middleware.certifier;

import java.io.Serializable;

public class OperationalSets implements Serializable {
    private BitOperationSet writeSet;
    private BitOperationSet readSet;

    public OperationalSets(BitOperationSet writeSet, BitOperationSet readSet){
        this.writeSet = writeSet;
        this.readSet = readSet;
    }

    public BitOperationSet getWriteSet(){
        return writeSet;
    }

    public BitOperationSet getReadSet(){
        return readSet;
    }

    public boolean intersectRead(BitOperationSet readSet){
        if (readSet == null || this.writeSet == null)
            return false;
        return readSet.intersects(this.writeSet);
    }

    public boolean intersectWrite(BitOperationSet writeSet){
        if (writeSet == null)
            return false;
        else if (this.writeSet != null && this.readSet != null)
            return writeSet.intersects(this.writeSet) || writeSet.intersects(this.readSet);
        else if (this.writeSet == null)
            return writeSet.intersects(this.readSet);
        else
            return writeSet.intersects(this.writeSet);
    }

    public boolean intersect(OperationalSets set){
        System.out.println("intersect write: " + intersectWrite(set.getWriteSet()));
        System.out.println("intersect read: " + intersectRead(set.getReadSet()));
        return intersectWrite(set.getWriteSet()) || intersectRead(set.getReadSet());
    }
}
