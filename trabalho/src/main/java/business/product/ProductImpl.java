package business.product;

import java.io.Serializable;
import java.util.Objects;

public class ProductImpl implements Product, Serializable {

	private String name;
	private float price;
	private String description;
	private int quantity;

	public ProductImpl(String name, float price, String description, int quantity){
		this.name = name;
		this.price = price;
		this.description = description;
		this.quantity = quantity;
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

	public int getQuantity() {
		return quantity;
	}
}