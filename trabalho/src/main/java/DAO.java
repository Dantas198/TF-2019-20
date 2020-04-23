public interface DAO<K,T> {

    T get(K key);
    boolean put(T obj);
    boolean delete(K key);
    boolean update(K key, T obj);
}
