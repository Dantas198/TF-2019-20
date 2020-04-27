package business.order;

import business.product.Product;

import java.util.Collection;
import java.util.Map;

public interface Order {

	String getId();

	Map<Product, Integer> getProducts();

	/**
	 * 
	 * @param prod
	 * @param quantity
	 */
	void addProduct(Product prod, int quantity);

	float getPrice();
}