package business;

import business.customer.Customer;
import business.order.Order;
import business.product.Product;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SuperMarket extends Serializable {

	boolean addCustomer(String customer);
	boolean resetOrder(String customer);
	boolean finishOrder(String customer);
	boolean addProduct(String customer, String name, int amount);
	Map<Product,Integer> getCurrentOrderProducts(String customer);
	List<Order> getHistory(String customer);
	ArrayList<Product> getCatalogProducts();
}