package business.product;

import java.io.Serializable;
import java.util.Objects;

public class ProductImpl implements Product, Serializable {

	private String name;
	private float price;
	private String description;
	private int stock;

	public ProductImpl(String name, float price, String description, int stock){
		this.name = name;
		this.price = price;
		this.description = description;
		this.stock = stock;
	}

	public ProductImpl(String name){
		this(name, 0, "", 0);
	}

	public ProductImpl(String name, float price, String description){
		this(name, price, description, 0);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public float getPrice() {
		return price;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void reduceStock(int quantity) {
		if(quantity > stock) return;
		stock -= quantity;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		ProductImpl product = (ProductImpl) o;
		return Objects.equals(name, product.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public int getStock() {
		return stock;
	}

	@Override
	public String toString() {
		return "Product {" +
				"name='" + name + '\'' +
				", price=" + price +
				", description='" + description + '\'' +
				", stock=" + stock +
				'}';
	}
}