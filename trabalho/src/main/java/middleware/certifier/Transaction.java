package middleware.certifier;

import java.util.HashMap;
import java.util.UUID;

public class Transaction<V> {
    private String id = UUID.randomUUID().toString();
    private HashMap<String, HashMap<Long, WriteSet<V>>> writeSets;
}
