package business.data.order;

import business.data.DAO;
import business.order.Order;
import business.order.OrderImpl;

import java.util.HashMap;
import java.util.Map;

public class OrderDAO implements DAO<String, Order> {

	//DUMMY VARIABLE
	Map<String, Order> orderMap;

	public OrderDAO(){
		this.orderMap = new HashMap<>();
	}

	/**
	 * 
	 * @param key
	 */
	public boolean delete(String key) {
		this.orderMap.remove(key);
		return true;
	}

	/**
	 * 
	 * @param key
	 */
	public Order get(String key) {
		return orderMap.get(key);
	}

	/**
	 * 
	 * @param obj
	 */
	public boolean put(Order obj) {
		orderMap.put(obj.getId(), obj);
		return true;
	}

	/**
	 * 
	 * @param key
	 * @param obj
	 */
	public boolean update(String key, Order obj) {
		orderMap.replace(key, obj);
		return true;
	}

	@Override
	public Map<String, Order> getAll() {
		return orderMap;
	}

}