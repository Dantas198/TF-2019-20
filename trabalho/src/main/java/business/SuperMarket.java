package business;

import business.customer.Customer;
import business.order.Order;
import business.product.Product;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;

public interface SuperMarket extends Serializable {

	boolean addCustomer(String customer) throws Exception;
	boolean resetOrder(String customer) throws Exception;
	boolean finishOrder(String customer) throws Exception;
	boolean addProduct(String customer, String product, int amount) throws Exception;
	Map<Product,Integer> getCurrentOrderProducts(String customer) throws Exception;
	Collection<Order> getHistory(String customer) throws Exception;
	Collection<Product> getCatalogProducts() throws Exception;
}