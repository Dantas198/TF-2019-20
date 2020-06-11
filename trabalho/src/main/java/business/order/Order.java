package business.order;

import business.product.Product;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

public interface Order extends Serializable {

	String getId();

	Map<Product, Integer> getProducts();

	void addProduct(Product prod, int quantity);

	float getPrice();

	Date getTimestamp();

	boolean reset();

	String getCustomerId();

	void setCustomerId(String id);
}