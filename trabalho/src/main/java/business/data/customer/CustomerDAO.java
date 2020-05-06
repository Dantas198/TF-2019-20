package business.data.customer;

import business.customer.Customer;
import business.data.DAO;

import java.util.HashMap;
import java.util.Map;

public class CustomerDAO implements DAO<String, Customer> {

	//DUMMY VARIABLE
	private Map<String, Customer> customerMap;

	public CustomerDAO(){
		this.customerMap = new HashMap<>();
	}

	/**
	 * 
	 * @param key
	 */
	public boolean delete(String key) {
		customerMap.remove(key);
		return true;
	}

	/**
	 * 
	 * @param key
	 */
	public Customer get(String key) {
		return customerMap.get(key);
	}

	/**
	 * 
	 * @param obj
	 */
	public boolean put(Customer obj) {
		customerMap.put(obj.getId(), obj);
		return true;
	}

	/**
	 * 
	 * @param key
	 * @param obj
	 */
	public boolean update(String key, Customer obj) {
		customerMap.replace(key, obj);
		return true;
	}

	@Override
	public Map<String, Customer> getAll() {
		return customerMap;
	}

}