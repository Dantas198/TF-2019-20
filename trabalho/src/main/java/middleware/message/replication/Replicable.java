package middleware.message.replication;

import java.io.Serializable;

public interface Replicable<V extends Serializable> {
    V getState();
}
