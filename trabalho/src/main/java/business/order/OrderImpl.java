package business.order;

import business.product.Product;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class OrderImpl implements Order {

	private String id;
	private Map<Product, Integer> products;

	public OrderImpl(){
		this(UUID.randomUUID().toString());
	}

	public OrderImpl(String id){
		this.id = id;
		this.products = new HashMap<>();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public Map<Product, Integer> getProducts() {
		return products;
	}

	@Override
	public void addProduct(Product prod, int quantity) {
		products.putIfAbsent(prod, 0);
		int oldQuantity = products.get(prod);
		products.replace(prod, oldQuantity + quantity);
	}
}