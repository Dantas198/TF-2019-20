package business.data.product;

import business.data.DAO;
import business.product.Product;
import business.product.ProductImpl;

import java.util.HashMap;
import java.util.Map;

public class ProductDAO implements DAO<String, Product> {

	// DUMMY VARIABLE
	private Map<String, Product> productMap;

	public ProductDAO(){
		this.productMap = new HashMap<>();
	}

	/**
	 * 
	 * @param key
	 */
	public boolean delete(String key) {
		productMap.remove(key);
		return true;
	}

	/**
	 * 
	 * @param key
	 */
	public Product get(String key) {
		return productMap.get(key);
	}

	/**
	 * 
	 * @param obj
	 */
	public boolean put(Product obj) {
		productMap.put(obj.getName(), obj);
		return true;
	}

	/**
	 * 
	 * @param key
	 * @param obj
	 */
	public boolean update(String key, Product obj) {
		productMap.replace(key, obj);
		return true;
	}

	@Override
	public Map<String, Product> getAll() {
		return productMap;
	}

}