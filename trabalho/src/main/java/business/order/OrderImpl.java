package business.order;

import business.product.Product;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class OrderImpl implements Order, Serializable{

	private String id;
	private Map<Product, Integer> products;

	public OrderImpl(){
		this(UUID.randomUUID().toString());
	}

	public OrderImpl(String id){
		this.id = id;
		this.products = new HashMap<>();
	}

	public OrderImpl(String id, Map<Product, Integer> products) {
		this.id = id;
		this.products = products;
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

	@Override
	public float getPrice() {
		float price = 0;
		for(Product prod : products.keySet()){
			price += prod.getPrice() * products.get(prod);
		}
		return price;
	}

	@Override
	public boolean reset() {
		this.products.clear();
		return true;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		OrderImpl order = (OrderImpl) o;
		return Objects.equals(id, order.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}