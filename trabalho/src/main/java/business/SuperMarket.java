package business;

import business.customer.Customer;
import business.order.Order;
import business.product.Product;

import java.io.Serializable;
import java.util.Collection;

public interface SuperMarket extends Serializable {

	/**
	 * 
	 * @param customer
	 */
	boolean addCustomer(Customer customer) throws Exception;

	/**
	 * 
	 * @param client
	 * @param prod
	 * @param quantity
	 */
	void addToOrder(Customer client, Product prod, int quantity) throws Exception;

	/**
	 * 
	 * @param client
	 */
	void buyOrder(Customer client) throws Exception;

	/**
	 * 
	 * @param client
	 */
	Order getOrder(Customer client) throws Exception;

	/**
	 * 
	 * @param name
	 */
	Product getProduct(String name) throws Exception;

	Collection<Product> getProducts() throws Exception;

}