package middleware.message.replication;

import middleware.Certifier.BitWriteSet;

import java.util.List;

public interface Certifiable{
    BitWriteSet getWriteSet();
    long getStartTimestamp();
    List<String> getTables();
}
