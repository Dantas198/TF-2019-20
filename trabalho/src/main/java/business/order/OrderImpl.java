package business.order;

import business.product.Product;

import java.io.Serializable;
import java.util.*;

public class OrderImpl implements Order, Serializable{

	private String id;
	private Map<Product, Integer> products;
	private Date timestamp;
	private String customerId;

	public OrderImpl(){
		this(UUID.randomUUID().toString());
	}

	public OrderImpl(String id){
		this.id = id;
		this.products = new HashMap<>();
		this.timestamp = Calendar.getInstance().getTime();
	}

	public OrderImpl(String id, Map<Product, Integer> products) {
		this.id = id;
		this.products = products;
		this.timestamp = Calendar.getInstance().getTime();
	}

	public OrderImpl(String id, Map<Product, Integer> products, Date timestamp) {
		this.id = id;
		this.products = products;
		this.timestamp = timestamp;
	}

	public OrderImpl(String id, Map<Product, Integer> products, Date timestamp, String customerId) {
		this.id = id;
		this.products = products;
		this.timestamp = timestamp;
		this.customerId = customerId;
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
		products.merge(prod, quantity, Integer::sum);
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

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String getCustomerId() {
		return this.customerId;
	}

	@Override
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	@Override
	public String toString() {
		return "OrderImpl{" +
				"id='" + id + '\'' +
				", products=" + products +
				", timestamp=" + timestamp +
				", customerId='" + customerId + '\'' +
				'}';
	}
}