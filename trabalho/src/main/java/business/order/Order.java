package business.order;

import business.product.Product;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public interface Order{

	String getId();

	Map<Product, Integer> getProducts();

	void addProduct(Product prod, int quantity);

	float getPrice();

	boolean reset();
}