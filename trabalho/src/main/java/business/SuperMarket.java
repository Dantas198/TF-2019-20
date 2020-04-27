package business;

import business.customer.Customer;
import business.order.Order;
import business.product.Product;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface SuperMarket extends Serializable {

	boolean startOrder(Customer customer);
	boolean finishOrder(Customer customer);
	boolean addProduct(String name, int amount);
	Collection<Product> getCurrentOrderProducts(Customer customer);
	List<Order> getHistory(Customer customer);
	Collection<Product> getCatalogProducts();


}