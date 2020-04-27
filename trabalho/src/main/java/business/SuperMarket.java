package business;

import business.customer.Customer;
import business.order.Order;
import business.product.Product;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;

public interface SuperMarket extends Serializable {

	boolean addCustomer(String customer) throws Exception;
	boolean resetOrder(String customer);
	boolean finishOrder(String customer) throws Exception;
	boolean addProduct(String customer, Product product, int amount);
	Map<Product,Integer> getCurrentOrderProducts(String customer);
	Collection<Order> getHistory(String customer) throws Exception;
	Collection<Product> getCatalogProducts() throws Exception;
}