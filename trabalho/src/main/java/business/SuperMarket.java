package business;

import business.order.Order;
import business.product.Product;

import java.io.Serializable;
import java.util.*;

public interface SuperMarket extends Serializable {

	boolean addCustomer(String customer);
	boolean resetOrder(String customer);
	boolean finishOrder(String customer);
	boolean addProductToOrder(String customer, String product, int amount);
	Map<Product,Integer> getCurrentOrderProducts(String customer);
	Collection<Order> getHistory(String customer);
	Collection<Product> getCatalogProducts();
	boolean updateProduct(String name, float price, String description, int stock);
}