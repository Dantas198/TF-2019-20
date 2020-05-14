package middleware.message.replication;

import middleware.Certifier.BitWriteSet;

public interface Certifiable{
    BitWriteSet getWriteSet();
    long getStartTimestamp();
}
