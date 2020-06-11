package business.data;

import middleware.certifier.StateUpdater;

import java.io.Serializable;
import java.util.Map;
import java.util.function.Function;

public class CertifierDAO<T extends Serializable> implements DAO<String, T> {
    private final DAO<String, T> dao;
    private final String tag;
    private final StateUpdater<String, Serializable> updater;
    private final Function<T, String> getKey;

    public CertifierDAO(DAO<String, T> dao,
                        String tag,
                        StateUpdater<String, Serializable> updater,
                        Function<T, String> getKey) {
        this.dao = dao;
        this.tag = tag;
        this.updater = updater;
        this.getKey = getKey;
    }

    @Override
    public T get(String key) {
        this.updater.read(tag, key);
        return dao.get(key);
    }

    @Override
    public Map<String, T> getAll() {
        Map<String, T> map = dao.getAll();
        for (String key:
                map.keySet()) {
            this.updater.read(tag, key);
        }
        return map;
    }

    @Override
    public boolean put(T obj) {
        this.updater.put(tag, getKey.apply(obj), obj);
        return true;
    }

    @Override
    public boolean delete(String key) {
        this.updater.put(tag, key, null);
        return true;
    }

    @Override
    public boolean update(String key, T obj) {
        put(obj);
        return true;
    }
}

