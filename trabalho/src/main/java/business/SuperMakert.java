package business;

import business.customer.Customer;
import business.order.Order;
import business.product.Product;

import java.util.Collection;

public interface SuperMakert {

	/**
	 * 
	 * @param customer
	 */
	boolean addCustomer(Customer customer);

	/**
	 * 
	 * @param client
	 * @param prod
	 * @param quantity
	 */
	void addToOrder(Customer client, Product prod, int quantity);

	/**
	 * 
	 * @param client
	 */
	void buyOrder(Customer client);

	/**
	 * 
	 * @param client
	 */
	Order getOrder(Customer client);

	/**
	 * 
	 * @param name
	 */
	Product getProduct(String name);

	Collection<Product> getProducts();

}