package business.data;

import java.util.Map;

public interface DAO<K,T> {

	/**
	 * 
	 * @param key
	 */
	T get(K key);

	/**
	 * 
	 * @param obj
	 */
	boolean put(T obj);

	/**
	 * 
	 * @param key
	 */
	boolean delete(K key);

	/**
	 * 
	 * @param key
	 * @param obj
	 */
	boolean update(K key, T obj);

	Map<K, T> getAll();
}